import 'dart:convert';
import 'package:http/http.dart' as http;
import '../constants/api_constants.dart';
import '../models/dashboard_model.dart';

class DashboardService {
  Future<DashboardModel> getDashboard(String token) async {
    final response = await http.get(
      Uri.parse(ApiConstants.dashboard),
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer $token',
      },
    );

    if (response.statusCode == 200) {
      return DashboardModel.fromJson(
          jsonDecode(response.body) as Map<String, dynamic>);
    }
    throw Exception('Erro ao carregar dashboard (HTTP ${response.statusCode})');
  }
}