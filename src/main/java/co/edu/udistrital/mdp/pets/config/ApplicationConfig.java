package co.edu.udistrital.mdp.pets.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {
	@Bean
	public ModelMapper modelMapper() {
		ModelMapper modelMapper = new ModelMapper();
		// The entity graph has crossing references (request -> trial -> request), so
		// some destination properties match more than one source hierarchy. Without
		// this the mapping fails with a ConfigurationException.
		modelMapper.getConfiguration().setAmbiguityIgnored(true);
		return modelMapper;
	}
}
