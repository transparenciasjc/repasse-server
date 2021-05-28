package org.sjcdigital.repasse.service.impl;

import java.util.List;

import javax.enterprise.context.Dependent;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;

import org.sjcdigital.repasse.model.base.Estado;
import org.sjcdigital.repasse.model.base.Municipio;
import org.sjcdigital.repasse.service.Service;

@Dependent
public class MunicipioService extends Service<Municipio> {

    public Municipio porEstadoNomeESIAFI(Estado estado, String nome, String siafi) {
        TypedQuery<Municipio> buscaMunicipios = em.createNamedQuery("Municipio.porEstadoNomeSIAFI", Municipio.class);
        buscaMunicipios.setParameter("estado", estado);
        buscaMunicipios.setParameter("siafi", siafi);
        buscaMunicipios.setParameter("nome", nome);
        return buscaMunicipios.getSingleResult();
    }

    @Transactional
    public Municipio porEstadoNomeESIAFIOuCria(Municipio municipio) {
        try {
            return porEstadoNomeESIAFI(municipio.getEstado(),
                                       municipio.getNome(),
                                       municipio.getCodigoSIAFI());
        } catch (NoResultException e) {
            this.salvar(municipio);
            return municipio;
        }
    }

    public Municipio buscaPorNomeEEstado(String sigla, String nome) {
        TypedQuery<Municipio> buscaMunicipios = em.createNamedQuery("Municipio.porNomeESigla", Municipio.class);
        buscaMunicipios.setParameter("sigla", sigla);
        buscaMunicipios.setParameter("nome", nome);
        return buscaMunicipios.getSingleResult();
    }

    public List<Municipio> porSiglaEstado(String sigla) {
        TypedQuery<Municipio> buscaMunicipios = em.createNamedQuery("Municipio.porSigla", Municipio.class);
        buscaMunicipios.setParameter("sigla", sigla);
        return buscaMunicipios.getResultList();
    }

    public void atualizaDadosGeograficos(long munId, String regiao, float lat, float lon) {
        Query atualizaRegiao = em.createNativeQuery("UPDATE municipio SET mun_regiao = :regiao, mun_latitude = :lat, mun_longitude = :lon WHERE mun_id = :munId");
        atualizaRegiao.setParameter("regiao", regiao);
        atualizaRegiao.setParameter("munId", munId);
        atualizaRegiao.setParameter("lat", lat);
        atualizaRegiao.setParameter("lon", lon);
        atualizaRegiao.executeUpdate();
    }

    public List<Municipio> buscaMunicipiosPorRegiao(String regiao) {
        TypedQuery<Municipio> buscaMunicipios = em.createNamedQuery("Municipio.porRegiao", Municipio.class);
        buscaMunicipios.setParameter("regiao", regiao);
        return buscaMunicipios.getResultList();
    }

}
