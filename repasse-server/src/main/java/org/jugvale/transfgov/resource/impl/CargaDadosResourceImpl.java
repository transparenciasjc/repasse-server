package org.jugvale.transfgov.resource.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.jugvale.transfgov.carga.CargaDadosGeograficosController;
import org.jugvale.transfgov.carga.CargaDadosPopController;
import org.jugvale.transfgov.carga.CargaDadosTransfController;
import org.jugvale.transfgov.carga.CargaIDHController;
import org.jugvale.transfgov.carga.CargaIndicadorController;
import org.jugvale.transfgov.model.base.AnoMes;
import org.jugvale.transfgov.model.carga.DadosCargaIndicador;
import org.jugvale.transfgov.model.carga.ResumoDadosTransferencia;
import org.jugvale.transfgov.model.transferencia.CargaTransfInfo;
import org.jugvale.transfgov.resource.CargaDadosResource;
import org.jugvale.transfgov.service.AnoService;
import org.jugvale.transfgov.service.impl.CargaTransfInfoService;
import org.jugvale.transfgov.service.impl.TransferenciaService;
import org.jugvale.transfgov.utils.ArquivoTransfUtils;

/**
 * 
 * Interface REST para permitir a carga de dados
 * 
 * @author wsiqueir
 *
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CargaDadosResourceImpl implements CargaDadosResource {

	@Inject
	TransferenciaService transferenciaService;

	@Inject
	Logger logger;

	@Inject
	CargaDadosTransfController cargaDadosController;

	@Inject
	CargaDadosPopController cargaDadosPopController;

	@Inject
	CargaTransfInfoService cargaTransfInfoService;

	@Inject
	CargaIDHController cargaIDHController;

	@Inject
	AnoService anoService;

	@Inject
	CargaDadosGeograficosController cargaMicroRegiaoController;

	@Inject
	CargaIndicadorController cargaIndicadorController;
	
	public Response baixaECarrega(int ano, int mes) throws IOException {
		verificaSeJaFoiCarregado(ano, mes);
		java.nio.file.Path arquivoCSV = null;
		try {
			logger.info(String.format(MSG_BAIXANDO_INICIO, ano, mes));
			arquivoCSV = ArquivoTransfUtils.baixaDeszipaECriaCSV(ano, mes);
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(MSG_ERRO_BAIXAR).build();
		}
		logger.info(String.format(MSG_SUCESSO_AO_BAIXAR, ano, mes));
		cargaDadosController.carregarNoBanco(ano, mes, arquivoCSV);
		return Response.ok(String.format(MSG_SUCESSO, ano, mes, ano, mes))
				.build();
	}

	/**
	 * Código macarrão para ler o zip de transferencia do site
	 * www.portaldatransparencia.gov.br
	 * 
	 * @param multipart
	 * @return
	 * @throws IOException
	 */
	public Response recebeCarga(MultipartInput conteudo) throws IOException {
		int ano = 0, mes = 0;
		InputPart conteudoZip = null;
		for (InputPart p : conteudo.getParts()) {
			if (p.getMediaType().toString()
					.equals(MediaType.APPLICATION_OCTET_STREAM)) {
				conteudoZip = p;
			}
		}
		Objects.requireNonNull(conteudoZip, "Você deve enviar um anexo");
		java.nio.file.Path arquivoCSV = ArquivoTransfUtils
				.deszipaECriaCSV(conteudoZip.getBody(byte[].class, null));
		String dadosData = arquivoCSV.toFile().getName().substring(0, 6);
		ano = Integer.parseInt(dadosData.substring(0, 4));
		mes = Integer.parseInt(dadosData.substring(4, 6));
		verificaSeJaFoiCarregado(ano, mes);
		cargaDadosController.carregarNoBanco(ano, mes, arquivoCSV);
		return Response.ok(String.format(MSG_SUCESSO, ano, mes, ano, mes))
				.build();
	}

	private void verificaSeJaFoiCarregado(int ano, int mes) {
		if (transferenciaService.temTranferencia(ano, mes))
			throw new WebApplicationException(Response.status(Status.CONFLICT)
					.entity(String.format(MSG_CARGA_REPETIDA, ano, mes))
					.build());
	}

	@Override
	public CargaTransfInfo recuperaInfoCarga(int ano, int mes) {
		return cargaTransfInfoService.porAnoMes(ano, mes);
	}

	@Override
	public List<CargaTransfInfo> todasInfoCargas() {
		return cargaTransfInfoService.todos();
	}

	@Override
	public Response cargaDadosPop() {
		try {
			return Response
					.ok(cargaDadosPopController.fazCargaDadosPopulacao())
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().entity(e.getMessage()).build();
		}

	}

	@Override
	public List<ResumoDadosTransferencia> resumoDadosTransferencia() {
		List<ResumoDadosTransferencia> resumos = new ArrayList<>();
		List<AnoMes> anos = anoService.anos();
		for (AnoMes anoMes : anos) {
			for (int mes : anoMes.getMeses()) {
				int ano = anoMes.getAno();
				ResumoDadosTransferencia resumo = new ResumoDadosTransferencia();
				resumo.setAno(ano);
				resumo.setMes(mes);
				resumo.setTotalTransferencias(transferenciaService
						.contaPorAnoMesMunicipio(ano, mes));
				try {
					resumo.setContagemLinhas(ArquivoTransfUtils
							.contaLinhasdoSite(ano, mes));
				} catch (IOException e) {
					resumo.setContagemLinhas(-1);
				}
				resumos.add(resumo);
			}
		}
		return resumos;
	}

	@Override
	public Response cargaDadosIDH() {
		logger.info("Iniciando carga de dados do IDH");
		try {
			return Response.ok(cargaIDHController.fazCargaIDH()).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().entity(e.getMessage()).build();
		}
	}

	@Override
	public Response cargaDadosRegiao() {
		logger.info("Iniciando carga de dados de Região");
		try {
			return Response.ok(cargaMicroRegiaoController.fazCargaDadosGeográficos())
					.build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().entity(e.getMessage()).build();
		}
	}

	@Override
	public Response cargaDadosIndicador(DadosCargaIndicador dadosCargaIndicador) {
		logger.info("Iniciando carga de dados de Indicador");
		cargaIndicadorController.carregaIndicadores(dadosCargaIndicador);
		return Response.ok("Carga de dados em andamento. Confira os logs").build();
	}

}