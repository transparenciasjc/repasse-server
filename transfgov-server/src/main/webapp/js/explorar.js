var appExplorar = angular.module('AppExplorar', [ 'datatables' ]);

function inicializa($scope, $http) {
	$('#abasPainel a').click(function(e) {
		e.preventDefault();
		$(this).tab('show');
	});
	$http.get("./rest/agregacao").success(function(agregacoes) {
		$scope.agregacoes =  $.grep(agregacoes, function(agregacao) {
			return agregacao != 'MUNICIPIO' && agregacao != 'MES' && agregacao != 'ANO';
		});
		 $scope.agregacaoSelecionada = $scope.agregacoes[0];
	});	

}

function criaGraficoAnoArea(agregacoesAno) {
	var categorias = new Array(); 
	var seriesMap = {};
	var ano;
	var series = new Array();
	agregacoesAno.forEach(function(agregacaoAno) {
		ano = agregacaoAno.ano;
		categorias.push(agregacaoAno.mes);
		for(a in agregacaoAno.dadosAgregados){
			if(!seriesMap[a]) seriesMap[a] = new Array();
			seriesMap[a].push(agregacaoAno.dadosAgregados[a]);
		}
	});	
	for(s in seriesMap) {
		series.push({
			name: s,
			data: seriesMap[s]
		});
	}
	  $('#divGraficoAreaPorAno').highcharts({
	        title: {
	            text: 'Tranferências no ano ' + ano,
	            x: -20 //center
	        },
	        subtitle: {
	            text: 'Agregadas todas as transferências no ano de ' + ano,
	            x: -20
	        },
	        xAxis: {
	        	title: {
	                text: 'Mês'
	            },
	            categories: categorias
	        },	        
	        tooltip: {
	        	valuePrefix: "R$ "	        	
	        },
	        yAxis: {
	            title: {
	                text: 'Valor'
	            },
	            plotLines: [{
	                value: 0,
	                width: 1,
	                color: '#808080'
	            }]
	        },
	        legend: {
	        	itemWidth: 200,
	            layout: 'vertical',
	            align: 'right',
	            verticalAlign: 'middle',
	            borderWidth: 0
	        },
	        series: series
	    });	
}

appExplorar.controller('ExplorarController', function($scope, $http) {
	inicializa($scope, $http);	
	$http.get("./rest/ano").success(function(anos) {
		$scope.anos = anos;
	});
	$http.get("./rest/estado").success(function(estados) {
		$scope.estados = estados;
	});
	$scope.carregaMunicipios = function() {
		var sigla = $scope.estadoSelecionado.sigla;
		var urlEstados = "./rest/estado/" + sigla + "/municipios";
		$http.get(urlEstados).success(function(municipios) {
			$scope.municipios = municipios;
		});
	}
	$scope.carregaApp = function() {
		$scope.carregaAgregacaoAno();
		$scope.municipioBusca = $scope.municipioSelecionado;
	}
	
	$scope.listenerAgregacao = function(){
		$scope.carregaAgregacaoAno();
		$scope.carregaDadosMes();
	}
	
	$scope.carregaAgregacaoAno = function(){
		var ano = $scope.anoSelecionado.ano;
		var id = $scope.municipioSelecionado.id;
		var agreg = $scope.agregacaoSelecionada;
		var uriTransfAno = "rest/agregacao/ANO/" + ano + "/" + agreg + "/municipio/" + id;
		$http.get(uriTransfAno).success(criaGraficoAnoArea);
	}
	
	$scope.carregaDadosMes = function() {
		var ano = $scope.anoSelecionado.ano;
		var mes = $scope.mesSelecionado;
		var id = $scope.municipioSelecionado.id;
		var uriTransfMes = "rest/transferencia/" + ano + "/" + mes + "/municipio/"
				+ id;
		var uriTransfAno = "rest/agregacao/ANO/" + ano + "/AREA/municipio/" + id;
		$http.get(uriTransfMes).success(function(transferencias) {
			$scope.transferenciasMes = transferencias;
			$scope.carregaGraficosAgregacao();
		});
	}
	
	$scope.carregaGraficosAgregacao = function() {		
		var a = $scope.agregacaoSelecionada;
		var ano = $scope.anoSelecionado.ano;
		var mes = $scope.mesSelecionado;
		var id = $scope.municipioSelecionado.id;
		if(!a) return;
		$scope.gerandoGraficoAgregacao = true;
		var uriAgregacao =  a + "/"+ ano+ "/" + mes + "/municipio/"+ id;
		$http.get("./rest/agregacao/" + uriAgregacao).success(function(agregacao) {
			$scope.dadosAgregados= agregacao.dadosAgregados;
			var dadosGrafico = new Array();
			for(i in agregacao.dadosAgregados) {
				dadosGrafico.push({
                    name: i,
                    y: agregacao.dadosAgregados[i]
                });
			}
			$('#containerPizzaAgregacao').highcharts({
		        chart: {
		            plotBackgroundColor: null,
		            plotBorderWidth: null,
		            plotShadow: false
		        },
		        title: {
		            text: 'Dados agregados por ' + a
		        },
		        tooltip: {
		            pointFormat: '<b>{point.percentage:.1f}%</b>'
		        },
		        legend: {
		        	itemWidth: 250,
		            layout: 'vertical',
		            align: 'right',
		            verticalAlign: 'middle',
		            borderWidth: 0
		        },
		        plotOptions: {
		            pie: {
		                allowPointSelect: true,
		                cursor: 'pointer',
		                dataLabels: {
		                    enabled: false,
		                },
	                    showInLegend: true
		            }
		        },
		        series: [{
		            type: 'pie',
		            name: 'Dados por ' + a,
		            data: dadosGrafico
		        }]
		    });
		});
		$scope.gerandoGraficoAgregacao = false;
	}

});