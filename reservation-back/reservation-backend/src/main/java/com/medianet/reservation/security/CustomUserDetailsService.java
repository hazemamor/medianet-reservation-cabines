package com.medianet.reservation.security;

import com.medianet.reservation.entity.Utilisateur;
import com.medianet.reservation.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + email));

        return User.builder()
                .username(utilisateur.getEmail())
                // Authentification passwordless : aucun mot de passe local, un mot de passe technique inutilisé est défini
                .password("")
                .authorities(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name()))
                .build();
    }
}
