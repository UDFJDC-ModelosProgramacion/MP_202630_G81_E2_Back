package co.edu.udistrital.mdp.pets.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import co.edu.udistrital.mdp.pets.entities.UserEntity;
import co.edu.udistrital.mdp.pets.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		UserEntity user = userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
		if (user.getPassword() == null || user.getRole() == null)
			throw new UsernameNotFoundException("Invalid email or password");
		return new AppUserPrincipal(user.getId(), user.getEmail(), user.getPassword(), user.getRole());
	}
}
