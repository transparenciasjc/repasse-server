package org.jugvale.transparencia.transf.resource;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.jugvale.transparencia.transf.agregacao.AgregacaoController;
import org.jugvale.transparencia.transf.model.agregacao.Agregacao;
import org.jugvale.transparencia.transf.model.agregacao.TipoAgregacao;
import org.jugvale.transparencia.transf.model.base.Area;
import org.jugvale.transparencia.transf.model.base.Estado;
import org.jugvale.transparencia.transf.model.base.Municipio;
import org.jugvale.transparencia.transf.model.transferencia.Transferencia;
import org.jugvale.transparencia.transf.service.impl.AreaService;
import org.jugvale.transparencia.transf.service.impl.EstadoService;
import org.jugvale.transparencia.transf.service.impl.MunicipioService;
import org.jugvale.transparencia.transf.service.impl.TransferenciaService;
import org.jugvale.transparencia.transf.utils.JaxrsUtils;

@Path("agregacao")
@Produces("application/json; charset=utf8")
public class AgregacaoResource {
	
	final String MSG_AGREGACAO_ERRADA = "A agregação %s não é permitida em agregações %s";
	final String MSG_NAO_HA_DADOS_ANO_MES =  "Não há dados de transferência para ano %d e mês %d";
	final String MSG_NAO_HA_DADOS_ANO  =  "Não há dados de transferência para ano %d";

	
	@Inject
	AgregacaoController agregacaoController;
	
	@Inject
	MunicipioService municipioService;
	
	@Inject
	TransferenciaService transferenciaService;

	@Inject
	EstadoService estadoService;

	@Inject
	private AreaService areaService;
	
	@GET
	public TipoAgregacao[] todasAgregacoes() {
		return TipoAgregacao.values();
	}
	
	@GET
	@Path("/{tipoAgregacao}/{ano}/{mes}/municipio/{idMunicipio}")	
	public Agregacao agregaPorAnoMesMunicipio(@PathParam("tipoAgregacao") TipoAgregacao tipoAgregacao, @PathParam("ano") int ano, @PathParam("mes") int mes, @PathParam("idMunicipio") long idMunicipio) {
		JaxrsUtils.lanca404SeFalso(transferenciaService.temTranferencia(ano, mes), String.format(MSG_NAO_HA_DADOS_ANO_MES, ano, mes));
		Municipio municipio = JaxrsUtils.lanca404SeNulo(municipioService.buscarPorId(idMunicipio), Municipio.class);
		List<Transferencia> transferencias = transferenciaService.buscarPorAnoMesMunicipio(ano, mes, municipio);
		return agregacaoController.agregaPorTipo(ano, mes, municipio.getEstado(), municipio, tipoAgregacao, transferencias);
	}
	
	@GET
	@Path("/{tipoAgregacao}/{ano}/municipio/{idMunicipio}")	
	public Agregacao agregaPorAnoMunicipio(@PathParam("tipoAgregacao") TipoAgregacao tipoAgregacao, @PathParam("ano") int ano, @PathParam("idMunicipio") long idMunicipio) {
		JaxrsUtils.lanca404SeFalso(transferenciaService.temTranferencia(ano), String.format(MSG_NAO_HA_DADOS_ANO, ano));
		Municipio municipio = JaxrsUtils.lanca404SeNulo(municipioService.buscarPorId(idMunicipio), Municipio.class);
		List<Transferencia> transferencias = transferenciaService.buscarPorAnoMunicipio(ano, municipio);
		return agregacaoController.agregaPorTipo(ano, 0, municipio.getEstado(), municipio, tipoAgregacao, transferencias);
	}
	
	@GET
	@Path("/AREA/area/{areaId}/{ano}/{mes}/municipio/{idMunicipio}")	
	public Agregacao agregaPorAnoMesAreaMunicipio(@PathParam("areaId") long idArea, @PathParam("ano") int ano, @PathParam("mes") int mes, @PathParam("idMunicipio") long idMunicipio) {
		JaxrsUtils.lanca404SeFalso(transferenciaService.temTranferencia(ano, mes), String.format(MSG_NAO_HA_DADOS_ANO_MES, ano, mes));
		Area area = JaxrsUtils.lanca404SeNulo(areaService.buscarPorId(idArea), Area.class);
		Municipio municipio = JaxrsUtils.lanca404SeNulo(municipioService.buscarPorId(idMunicipio), Municipio.class);
		List<Transferencia> transferencias = transferenciaService.buscarPorAnoMesAreaMunicipio(ano, mes, area, municipio);
		return agregacaoController.agregaPorTipo(ano, 0, municipio.getEstado(), municipio, TipoAgregacao.SUB_FUNCAO, transferencias);
	}
	
	@GET
	@Path("/MUNICIPIO/{ano}/{mes}/estado/{siglaEstado}")	
	public Agregacao agregaPorAnoEstado(@PathParam("ano") int ano,  @PathParam("mes") int mes, @PathParam("siglaEstado") String siglaEstado) {
		Estado estado = JaxrsUtils.lanca404SeNulo(estadoService.buscaEstadoPorSigla(siglaEstado), Estado.class);
		List<Transferencia> transferencias = transferenciaService.buscarPorAnoMesEstado(ano, mes, estado);
		return agregacaoController.agregaPorTipo(ano, 0, estado, null, TipoAgregacao.MUNICIPIO, transferencias);
	}

}