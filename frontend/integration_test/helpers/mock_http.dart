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

    if (path.endsWith('/transactions') && request.method == 'POST') {
      return http.Response(novaTransacaoJson, 201,
          headers: {'content-type': 'application/json'});
    }

    if (path.contains('/transactions/') && request.method == 'DELETE') {
      return http.Response('', 204);
    }

    if (path.endsWith('/orcamentos') || path.endsWith('/metas')) {
      return http.Response('[]', 200,
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
