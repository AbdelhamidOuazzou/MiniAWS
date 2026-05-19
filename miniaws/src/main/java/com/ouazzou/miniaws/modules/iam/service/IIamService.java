package com.ouazzou.miniaws.modules.iam.service;

import com.ouazzou.miniaws.modules.iam.domain.AppUser;

public interface IIamService {
    // Appelée par le Filtre de Sécurité quand un Token arrive
    AppUser getOrCreateUser(String firebaseUid, String email);

    // Appelée par le Controller quand l'appli Android veut afficher le profil
    AppUser getUserProfile(String firebaseUid);
}