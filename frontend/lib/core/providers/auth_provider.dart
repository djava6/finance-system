import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/foundation.dart';

class AuthProvider extends ChangeNotifier {
  User? _user;
  String? _token;

  AuthProvider() {
    // idTokenChanges fires on sign-in, sign-out, and token refresh
    FirebaseAuth.instance.idTokenChanges().listen(_onIdTokenChanged);
  }

  Future<void> _onIdTokenChanged(User? user) async {
    _user = user;
    if (user != null) {
      _token = await user.getIdToken();
      if (kDebugMode) debugPrint('--- Firebase ID Token ---\n$_token\n---');
    } else {
      _token = null;
    }
    notifyListeners();
  }

  User? get user => _user;
  String? get token => _token;
  String? get nome => _user?.displayName ?? _user?.email;
  String? get email => _user?.email;
  bool get isAuthenticated => _user != null && _token != null;

  Future<String?> getToken() async {
    return await _user?.getIdToken();
  }

  Future<void> signInWithGoogle() async {
    final provider = GoogleAuthProvider();
    if (kIsWeb) {
      await FirebaseAuth.instance.signInWithPopup(provider);
    } else {
      await FirebaseAuth.instance.signInWithProvider(provider);
    }
  }

  void updateNome(String nome) {
    FirebaseAuth.instance.currentUser?.updateDisplayName(nome);
    notifyListeners();
  }

  Future<void> logout() async {
    await FirebaseAuth.instance.signOut();
  }
}
