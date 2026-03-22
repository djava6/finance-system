import 'package:finance_app/core/providers/auth_provider.dart';

/// Simulates an authenticated user without touching Firebase.
class MockAuthProvider extends AuthProvider {
  final bool _authenticated;

  MockAuthProvider({bool authenticated = true})
      : _authenticated = authenticated,
        super.skip();

  @override
  bool get isAuthenticated => _authenticated;

  @override
  bool get isLocked => false;

  @override
  String? get nome => 'Usuário Teste';

  @override
  String? get email => 'teste@exemplo.com';

  @override
  Future<void> logout() async {}
}
