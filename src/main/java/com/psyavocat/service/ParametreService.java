package com.psyavocat.service;

import com.psyavocat.dto.admin.ParametrePlateformeDTO;

public interface ParametreService {
    ParametrePlateformeDTO getParametres();
    ParametrePlateformeDTO updateParametres(ParametrePlateformeDTO dto);
}
