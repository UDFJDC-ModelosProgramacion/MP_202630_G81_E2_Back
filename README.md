# Proyecto base para el Backend. Curso Modelos de Programación

## Enlaces de interés
- [Jenkins](http://200.69.103.29:8085/jenkins/) -> Ingrese con sus credenciales de GitHub
- [SonarQube](http://200.69.103.29:8084/sonar/) -> No requiere credenciales

## Contraseñas
Las contraseñas nuevas de usuarios y adoptantes se almacenan con BCrypt; para
verificarlas debe usarse `PasswordEncoder.matches`, nunca comparar el texto
ingresado directamente con el valor persistido. Las contraseñas existentes no
se migran automáticamente: antes de desplegar sobre datos previos, debe
planificarse una migración controlada o un restablecimiento de credenciales.

## Seguridad de la API

### Autenticación de la SPA

La API usa sesiones de servidor con cookies (no JWT):

1. `GET /api/auth/csrf` entrega `{"token":"..."}` y establece la cookie
   `XSRF-TOKEN`. La SPA debe enviar su valor en `X-XSRF-TOKEN` en cada
   `POST`, `PUT`, `PATCH` o `DELETE`.
2. `POST /api/auth/login` recibe
   `{"email":"usuario@example.com","password":"..."}`. Responde `200` con
   `{"id":123,"email":"usuario@example.com","role":"ADOPTER","shelterId":null}`
   y establece `JSESSIONID` (`HttpOnly`). `shelterId` se lee de la asociación
   persistida del usuario; es `null` si no tiene refugio asociado. La contraseña
   se compara con BCrypt y no se devuelve.
3. `GET /api/auth/me` devuelve los mismos campos de la identidad autenticada y
   la asociación de refugio actual del servidor.
   `POST /api/auth/logout` invalida la sesión; responde `204` y requiere CSRF.

La sesión expira tras 30 minutos de inactividad por defecto; la actividad
renueva ese límite. No hay token de renovación: al expirar, la SPA vuelve a
iniciar sesión. Las cookies son `HttpOnly`, `Secure` por defecto y
`SameSite=Lax` por defecto. Para un frontend realmente cross-site configure
`SESSION_COOKIE_SAME_SITE=None` y mantenga `SESSION_COOKIE_SECURE=true` bajo
HTTPS.

En solicitudes de sesión ausente o inválida la API responde `401`; si el usuario
autenticado no tiene el rol o la propiedad requeridos responde `403`. Ambos
usan el formato `apierror.message`.

### Roles y matriz de permisos

Los roles persistidos son `ADMIN`, `SHELTER_ADMIN`, `VETERINARIAN` y `ADOPTER`.
Se asignan en el servidor; el registro nunca acepta un rol del cliente.

| Rol | Permisos principales |
| --- | --- |
| `ADMIN` | Operaciones administrativas en cualquier refugio y recursos; asignación de roles. |
| `SHELTER_ADMIN` | Administración de su refugio y sus mascotas/eventos/fotos; acceso clínico al mismo refugio. El backend compara el refugio asociado a la sesión con el del recurso. |
| `VETERINARIAN` | Operaciones clínicas y seguimientos asociados al refugio del veterinario. |
| `ADOPTER` | Su propio perfil, notificaciones, seguimientos de sus adopciones y reseñas de sus adopciones. |

Las lecturas públicas se limitan a salud, catálogo de mascotas/refugios/
veterinarios/eventos, reseñas y vacunas de catálogo sin filtros de registros
clínicos. Lecturas de perfiles de usuarios/adoptantes, eventos médicos,
vacunación y seguimientos requieren sesión y comprobación de propiedad. El
resto de rutas requiere autenticación; las operaciones de escritura además
requieren rol y, cuando aplica, propiedad del recurso. Los parámetros
`requesterId` y `role` en seguimientos se conservan por compatibilidad, pero no
participan en la autorización. Un administrador de refugio tampoco puede
transferir una mascota propia a otro refugio mediante el JSON de actualización.

En concreto, son públicas `GET /api/health`, `GET /api/pets` y
`GET /api/pets/{id}`, `GET /api/shelters` y `GET /api/shelters/{id}`,
`GET /api/veterinarians` y `GET /api/veterinarians/{id}`,
`GET /api/events` y `GET /api/events/{id}`, `GET /api/reviews`,
`GET /api/pets/{petId}/reviews/{reviewId}`, las lecturas de fotos de mascotas,
`GET /api/vaccines` sin filtros clínicos, `GET /api/auth/csrf`,
`POST /api/auth/login`, `POST /api/users` y `POST /api/adopters`.
Las rutas no enumeradas requieren autenticación; las anotaciones de método
añaden comprobaciones de rol y propiedad incluso bajo prefijos de lectura
pública como `/api/pets/**`.

| Operación | Acceso |
| --- | --- |
| Crear usuario/adoptante | Pública; crea siempre `ADOPTER`. |
| Ver/editar/eliminar usuario | Titular o `ADMIN`; asignar roles solo `ADMIN`. |
| Listar adoptantes | `ADMIN`; ver/editar/eliminar uno, `ADMIN`, titular o administrador del refugio asociado. |
| Crear refugio o veterinario | `ADMIN`; actualizar/eliminar refugio, `ADMIN` o `SHELTER_ADMIN` de ese refugio; actualizar/eliminar veterinario solo `ADMIN`. |
| Crear/editar/eliminar mascota, foto o evento de refugio | `ADMIN` o `SHELTER_ADMIN` del refugio asociado. |
| Leer catálogo y reseñas | Público; escribir reseñas, titular adoptante de la adopción o `ADMIN`. |
| Leer/escribir datos clínicos | `ADMIN`, `VETERINARIAN` del refugio o `SHELTER_ADMIN` del refugio; la lista global de eventos médicos y registros de vacunación es solo `ADMIN`. |
| Leer seguimientos | `ADMIN`, titular adoptante de la adopción, o personal clínico/administrativo de su refugio; crearlos/editar/eliminarlos, `ADMIN` o personal de ese refugio. |
| Leer y modificar notificaciones | Titular o `ADMIN`. |

`POST /api/users` y `POST /api/adopters` son registros públicos y siempre crean
cuentas `ADOPTER`; los campos de contraseña son de solo escritura y se guardan
con BCrypt. `POST /api/veterinarians` requiere `ADMIN`, recibe la contraseña
solo para escritura y asigna `VETERINARIAN` en el servidor.

El primer administrador se provisiona fuera de banda: tras aplicar la
migración, un operador autorizado debe identificar una cuenta verificada y
actualizar explícitamente su fila en `user_entity` a `ADMIN`, comprobando que
la operación afectó exactamente una cuenta. No existe una credencial inicial
ni una ruta pública de bootstrap. Después, `ADMIN` puede asignar roles con
`PUT /api/users/{id}/role` y JSON
`{"role":"SHELTER_ADMIN","shelterId":123}`. Para una cuenta veterinaria se usa
`{"role":"VETERINARIAN","shelterId":123,"password":"nueva-clave-segura"}`;
el backend solo permite ese rol para perfiles veterinarios y vuelve a hashear
la contraseña. `ADMIN` no puede degradar ni eliminar al último administrador.

Para una base de datos existente, ejecute una vez
`src/main/resources/db/manual/V2__add_user_role.sql` antes de iniciar con el
perfil de producción. Las cuentas existentes se migran como `ADOPTER`, salvo
los perfiles veterinarios; la asignación del primer administrador sigue siendo
manual y controlada.

### Configuración de despliegue y CORS

Active el perfil `prod` y proporcione, sin valores predeterminados,
`DB_URL`, `DB_DRIVER`, `DB_USERNAME` y `DB_PASSWORD`. El perfil de producción
desactiva H2 Console y usa `ddl-auto=validate`. Debe incluirse en el despliegue
el driver JDBC correspondiente a la base elegida. No se ha fijado un motor de
producción en este repositorio.

Configure `APP_CORS_ALLOWED_ORIGINS` con orígenes explícitos separados por
comas; en desarrollo se permite `http://localhost:5173` por defecto y
producción requiere configurar la variable con el origen real, por ejemplo
`https://app.example.com`. No use `*` con cookies. La SPA
debe enviar solicitudes con credenciales (`credentials: "include"`). Variables
adicionales: `SESSION_TIMEOUT` (por defecto `30m`),
`SESSION_COOKIE_SECURE` (por defecto `true`) y
`SESSION_COOKIE_SAME_SITE` (por defecto `Lax`). En desarrollo HTTP local,
configure explícitamente `SESSION_COOKIE_SECURE=false`; no aplique ese ajuste
en producción.

La suite Newman inicia la aplicación en el perfil `integration-tests`, que
queda enlazado a `127.0.0.1` y crea una cuenta ADMIN efímera usando
`POSTMAN_ADMIN_EMAIL` y `POSTMAN_ADMIN_PASSWORD` (mínimo 8 caracteres). Defina
ambas variables al ejecutar `.\mvnw.cmd verify`; no se configuran credenciales
predeterminadas ni se crea esa cuenta en producción. La colección `Security`
debe ejecutarse primero en Postman; Newman la combina al inicio, obtiene el
token CSRF, verifica respuestas 401/403, inicia sesión y deja la sesión lista
para las demás colecciones protegidas.
