import 'dart:convert';
import '../constants/api_constants.dart';
import '../models/orcamento_model.dart';
import 'api_client.dart';

class OrcamentoService {
  final _client = ApiClient();

  Future<List<OrcamentoModel>> listarPorMes(int mes, int ano) async {
    final uri = Uri.parse('${ApiConstants.orcamentos}/mes').replace(
      queryParameters: {'mes': '$mes', 'ano': '$ano'},
    );
    final response = await _client.get(uri);
    if (response.statusCode == 200) {
      final list = jsonDecode(response.body) as List;
      return list.map((e) => OrcamentoModel.fromJson(e as Map<String, dynamic>)).toList();
    }
    throw Exception('Erro ao carregar orçamentos');
  }

  Future<OrcamentoModel> criar({
    int? categoriaId,
    required double valorLimite,
    required int mes,
    required int ano,
  }) async {
    final body = <String, dynamic>{
      'valorLimite': valorLimite,
      'mes': mes,
      'ano': ano,
    };
    if (categoriaId != null) body['categoriaId'] = categoriaId;

    final response = await _client.post(
      Uri.parse(ApiConstants.orcamentos),
      body: jsonEncode(body),
    );
    if (response.statusCode == 201) {
      return OrcamentoModel.fromJson(jsonDecode(response.body) as Map<String, dynamic>);
    }
    throw Exception(_extractError(response.body));
  }

  Future<void> deletar(int id) async {
    await _client.delete(Uri.parse('${ApiConstants.orcamentos}/$id'));
  }

  String _extractError(String body) {
    try {
      final json = jsonDecode(body) as Map<String, dynamic>;
      return json['erro'] as String? ?? 'Erro desconhecido';
    } catch (_) {
      return 'Erro ao conectar com o servidor';
    }
  }
}
