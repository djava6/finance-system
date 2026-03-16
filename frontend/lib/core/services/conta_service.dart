import 'dart:convert';
import 'package:http/http.dart' as http;
import '../constants/api_constants.dart';
import '../models/conta_model.dart';

class ContaService {
  Map<String, String> _headers(String token) => {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      };

  Future<List<ContaModel>> listar(String token) async {
    final response = await http.get(
      Uri.parse(ApiConstants.contas),
      headers: _headers(token),
    );
    if (response.statusCode == 200) {
      final list = jsonDecode(response.body) as List;
      return list.map((e) => ContaModel.fromJson(e)).toList();
    }
    throw Exception('Erro ao carregar contas');
  }

  Future<ContaModel> criar(String token, String nome, double saldo) async {
    final response = await http.post(
      Uri.parse(ApiConstants.contas),
      headers: _headers(token),
      body: jsonEncode({'nome': nome, 'saldo': saldo}),
    );
    if (response.statusCode == 201) {
      return ContaModel.fromJson(jsonDecode(response.body));
    }
    throw Exception(_extractError(response.body));
  }

  Future<ContaModel> atualizar(
      String token, int id, String nome, double saldo) async {
    final response = await http.put(
      Uri.parse('${ApiConstants.contas}/$id'),
      headers: _headers(token),
      body: jsonEncode({'nome': nome, 'saldo': saldo}),
    );
    if (response.statusCode == 200) {
      return ContaModel.fromJson(jsonDecode(response.body));
    }
    throw Exception(_extractError(response.body));
  }

  Future<void> deletar(String token, int id) async {
    final response = await http.delete(
      Uri.parse('${ApiConstants.contas}/$id'),
      headers: _headers(token),
    );
    if (response.statusCode != 204) {
      throw Exception(_extractError(response.body));
    }
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
