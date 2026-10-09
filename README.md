# Proyecto base para el Backend. Curso Modelos de Programación

## Pruebas Postman de mascotas

La colección `Pet` usa un refugio y un usuario de prueba sembrados al iniciar la aplicación con el perfil `postman`. Desde PowerShell, inicia la API:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postman"
```

Después ejecuta toda la colección `Pet.postman_collection.json` con el entorno `EntornoColecciones` (base URL `http://localhost:8999/api`). La primera solicitud obtiene el ID real del refugio de prueba; no hace falta ejecutar primero la colección `Shelter`. Los datos se guardan en la base H2 en memoria y se vuelven a crear al reiniciar la aplicación.

## Enlaces de interés
- [Jenkins](http://200.69.103.29:8085/jenkins/) -> Ingrese con sus credenciales de GitHub
- [SonarQube](http://200.69.103.29:8084/sonar/) -> No requiere credenciales
