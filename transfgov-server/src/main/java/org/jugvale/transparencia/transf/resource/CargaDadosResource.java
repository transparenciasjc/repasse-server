package org.jugvale.transparencia.transf.resource;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartInput;
import org.jugvale.transparencia.transf.carga.CargaDadosController;
import org.jugvale.transparencia.transf.carga.MensagensCargaSingleton;
import org.jugvale.transparencia.transf.service.impl.TransferenciaService;
import org.jugvale.transparencia.transf.utils.ArquivoTransfUtils;

/**
 * 
 * Interface REST para permitir a carga de dados
 * 
 * @author wsiqueir
 *
 */
@Path("carga")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CargaDadosResource {
	
	final String MSG_SUCESSO = "Dados para ano %d e mês %d agendados para carga. Acesse GET /carga/%d/%d/mensagens para ver o andamento.";
	final String MSG_CARGA_REPETIDA = "Já existem dados para ano %d  mes %d. Limpe os dados antes de tentar uma nova carga";
	final String MSG_ERRO_BAIXAR = "Erro ao baixar dados. Verifique se o ano e mês estão disponíveis no portal da transparência. Mais detalhes no log do servidor";

	@Inject
	TransferenciaService transferenciaService;
	
	@Inject
	Logger logger;

	@Inject
	CargaDadosController cargaDadosController;

	@Inject
	MensagensCargaSingleton mensagens;
	
	
	@POST
	@Path("{ano}/{mes}")
	public Response baixaECarrega(@PathParam("ano") int ano, @PathParam("mes") int mes) throws IOException {
		verificaSeJaFoiCarregado(ano, mes);				
		java.nio.file.Path arquivoCSV = null;
		try {
			mensagens.adicionar(ano, mes, "Iniciando download de dados do portal da transparência em " + new Date());
			arquivoCSV = ArquivoTransfUtils.baixaDeszipaECriaCSV(ano, mes);
		} catch (Exception e) {
			mensagens.adicionar(ano, mes, "Problemas ao baixar e salvar dados, carga interrompida");
			e.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(MSG_ERRO_BAIXAR).build();
		}
		cargaDadosController.carregarNoBanco(ano, mes, arquivoCSV);
		return Response.ok(String.format(MSG_SUCESSO, ano, mes, ano, mes)).build();	
		
	}

	/**
	 * Código macarrão para ler o zip de transferencia do site www.portaldatransparencia.gov.br
	 * 
	 * @param multipart
	 * @return
	 * @throws IOException
	 */
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response recebeCarga(MultipartInput conteudo) throws IOException {
		int ano = 0, mes = 0;
		InputPart conteudoZip = null;
		for(InputPart p : conteudo.getParts()) {
			if(p.getMediaType().toString().equals(MediaType.APPLICATION_OCTET_STREAM)) {
				conteudoZip = p;
			}
		}
		Objects.requireNonNull(conteudoZip, "Você deve enviar um anexo");
		java.nio.file.Path arquivoCSV = ArquivoTransfUtils.deszipaECriaCSV(conteudoZip.getBody(byte[].class, null));
		String dadosData = arquivoCSV.toFile().getName().substring(0, 6);
		ano = Integer.parseInt(dadosData.substring(0, 4));
		mes = Integer.parseInt(dadosData.substring(4, 6));		
		verificaSeJaFoiCarregado(ano, mes);
		cargaDadosController.carregarNoBanco(ano, mes, arquivoCSV);
		return Response.ok(String.format(MSG_SUCESSO, ano, mes, ano, mes)).build();	
	}

	@GET
	@Path("/{ano}/{mes}/mensagens")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public List<String> recuperaMensagensCarga(@PathParam("ano") int ano,
			@PathParam("mes") int mes) {
		return mensagens.recuperarMensages(ano, mes);
	}

	@DELETE
	@Path("/{ano}/{mes}/mensagens")
	public void limpaMensages(@PathParam("ano") int ano,
			@PathParam("mes") int mes) {
		mensagens.limpar(ano, mes);
	}

	@DELETE
	@Path("/mensages")
	public void limpaMensages() {
		mensagens.limpar();
	}
	
	private void verificaSeJaFoiCarregado(int ano, int mes) {
		if (transferenciaService.temTranferencia(ano, mes)) 
			throw new WebApplicationException(Response.status(Status.CONFLICT).entity(String.format(MSG_CARGA_REPETIDA, ano, mes)).build());
	}

}