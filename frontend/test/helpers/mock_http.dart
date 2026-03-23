import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'fixtures.dart';

/// Returns a [MockClient] that intercepts all API calls and returns fixtures.
MockClient createMockHttpClient() {
  return MockClient((request) async {
    final path = request.url.path;

    if (path.endsWith('/dashboard')) {
      return http.Response(dashboardJson, 200,
          headers: {'content-type': 'application/json'});
    }

    if (path.endsWith('/transactions') && request.method == 'POST') {
      return http.Response(novaTransacaoJson, 201,
          headers: {'content-type': 'application/json'});
    }

    if (path.endsWith('/transactions') || path.contains('/transactions?')) {
      return http.Response(transacoesJson, 200,
          headers: {'content-type': 'application/json'});
    }

    if (path.endsWith('/categories')) {
      if (request.method == 'POST') {
        return http.Response('{"id":99,"nome":"Nova Categoria"}', 201,
            headers: {'content-type': 'application/json'});
      }
      return http.Response(categoriasJson, 200,
          headers: {'content-type': 'application/json'});
    }

    if (path.endsWith('/contas') || path.endsWith('/accounts')) {
      return http.Response(contasJson, 200,
          headers: {'content-type': 'application/json'});
    }

    if (path.contains('/transactions/') && request.method == 'DELETE') {
      return http.Response('', 204);
    }

    if (path.contains('/transactions/') && request.method == 'PUT') {
      return http.Response(novaTransacaoJson, 200,
          headers: {'content-type': 'application/json'});
    }

    if (path.endsWith('/users/me') && request.method == 'PUT') {
      return http.Response(
          '{"uid":"test-uid","email":"teste@exemplo.com","nome":"Usuário Atualizado"}',
          200,
          headers: {'content-type': 'application/json'});
    }

    if (path.contains('/contas/') && request.method == 'PUT') {
      return http.Response(contasJson.replaceAll('[', '').replaceAll(']', ''), 200,
          headers: {'content-type': 'application/json'});
    }

    if (path.contains('/metas/') && request.method == 'POST') {
      return http.Response('{"id":1,"nome":"Reserva de emergência","valorAlvo":10000.00,"valorAtual":3500.00,"percentual":35.0,"concluida":false}', 200,
          headers: {'content-type': 'application/json'});
    }

    if (path.endsWith('/orcamentos')) {
      if (request.method == 'POST') {
        return http.Response('{"id":99,"categoriaId":1,"categoria":"Alimentação","valorLimite":1000.00,"valorGasto":0,"percentual":0,"mes":3,"ano":2026}', 201,
            headers: {'content-type': 'application/json'});
      }
      return http.Response(orcamentosJson, 200,
          headers: {'content-type': 'application/json'});
    }

    if (path.endsWith('/metas')) {
      if (request.method == 'POST') {
        return http.Response('{"id":99,"nome":"Nova Meta","valorAlvo":1000.00,"valorAtual":0,"percentual":0,"concluida":false}', 201,
            headers: {'content-type': 'application/json'});
      }
      return http.Response(metasJson, 200,
          headers: {'content-type': 'application/json'});
    }

    if (path.endsWith('/users/me')) {
      return http.Response(
          '{"uid":"test-uid","email":"teste@exemplo.com","nome":"Usuário Teste"}',
          200,
          headers: {'content-type': 'application/json'});
    }

    // fallback
    return http.Response('{}', 200,
        headers: {'content-type': 'application/json'});
  });
}
