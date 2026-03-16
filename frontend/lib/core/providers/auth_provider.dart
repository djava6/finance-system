import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/auth_response.dart';
import '../services/auth_service.dart';

class AuthProvider extends ChangeNotifier {
  final AuthService _authService = AuthService();

  String? _token;
  String? _refreshToken;
  String? _nome;
  String? _email;

  String? get token => _token;
  String? get nome => _nome;
  String? get email => _email;
  bool get isAuthenticated => _token != null;

  Future<void> loadSession() async {
    final prefs = await SharedPreferences.getInstance();
    _token = prefs.getString('token');
    _refreshToken = prefs.getString('refreshToken');
    _nome = prefs.getString('nome');
    _email = prefs.getString('email');
    notifyListeners();
  }

  Future<void> login(String email, String senha) async {
    final response = await _authService.login(email, senha);
    await _saveSession(response);
  }

  Future<void> register(String nome, String email, String senha) async {
    final response = await _authService.register(nome, email, senha);
    await _saveSession(response);
  }

  Future<void> loginWithGoogle() async {
    final response = await _authService.loginWithGoogle();
    await _saveSession(response);
  }

  void updateNome(String nome) {
    _nome = nome;
    SharedPreferences.getInstance().then((p) => p.setString('nome', nome));
    notifyListeners();
  }

  Future<bool> tryRefreshToken() async {
    if (_refreshToken == null) return false;
    try {
      final response = await _authService.refreshToken(_refreshToken!);
      await _saveSession(response);
      return true;
    } catch (_) {
      await logout();
      return false;
    }
  }

  Future<void> logout() async {
    if (_token != null) {
      try {
        await _authService.logout(_token!);
      } catch (_) {}
    }
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
    _token = null;
    _refreshToken = null;
    _nome = null;
    _email = null;
    notifyListeners();
  }

  Future<void> _saveSession(AuthResponse response) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('token', response.token);
    await prefs.setString('refreshToken', response.refreshToken);
    await prefs.setString('nome', response.nome);
    await prefs.setString('email', response.email);
    _token = response.token;
    _refreshToken = response.refreshToken;
    _nome = response.nome;
    _email = response.email;
    notifyListeners();
  }
}
