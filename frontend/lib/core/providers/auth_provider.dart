import 'dart:convert';
import 'dart:math';
import 'package:crypto/crypto.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:sign_in_with_apple/sign_in_with_apple.dart';
import '../services/api_client.dart';
import '../services/user_service.dart';

class AuthProvider extends ChangeNotifier {
  User? _user;
  String? _token;
  bool _locked = false;

  AuthProvider() {
    ApiClient.onUnauthorized = logout;
    FirebaseAuth.instance.idTokenChanges().listen(_onIdTokenChanged);
  }

  /// Constructor for test subclasses — skips Firebase subscription.
  AuthProvider.skip();


  Future<void> _onIdTokenChanged(User? user) async {
    _user = user;
    if (user != null) {
      _token = await user.getIdToken();
      _registerFcmToken();
    } else {
      _token = null;
      _locked = false;
    }
    notifyListeners();
  }

  Future<void> _registerFcmToken() async {
    if (kIsWeb) return;
    try {
      final messaging = FirebaseMessaging.instance;
      final settings = await messaging.requestPermission();
      if (settings.authorizationStatus == AuthorizationStatus.authorized ||
          settings.authorizationStatus == AuthorizationStatus.provisional) {
        final fcmToken = await messaging.getToken();
        if (fcmToken != null) {
          await UserService().registerFcmToken(fcmToken);
        }
        messaging.onTokenRefresh.listen((newToken) {
          UserService().registerFcmToken(newToken);
        });
      }
    } catch (_) {
      // FCM registration is best-effort; do not interrupt login flow
    }
  }

  User? get user => _user;
  String? get token => _token;
  String? get nome => _user?.displayName ?? _user?.email;
  String? get email => _user?.email;
  bool get isAuthenticated => _user != null && _token != null;
  bool get isLocked => _locked && isAuthenticated;

  Future<String?> getToken() async {
    return await _user?.getIdToken();
  }

  // ── Lock / Unlock ───────────────────────────────────────────────────────────

  void lock() {
    if (isAuthenticated && !kIsWeb) {
      _locked = true;
      notifyListeners();
    }
  }

  void unlock() {
    _locked = false;
    notifyListeners();
  }

  // ── Sign-in methods ─────────────────────────────────────────────────────────

  Future<void> signInWithGoogle() async {
    final provider = GoogleAuthProvider();
    if (kIsWeb) {
      await FirebaseAuth.instance.signInWithPopup(provider);
    } else {
      await FirebaseAuth.instance.signInWithProvider(provider);
    }
  }

  Future<void> signInWithApple() async {
    final rawNonce = _generateNonce();
    final nonce = _sha256ofString(rawNonce);

    final appleCredential = await SignInWithApple.getAppleIDCredential(
      scopes: [
        AppleIDAuthorizationScopes.email,
        AppleIDAuthorizationScopes.fullName,
      ],
      nonce: nonce,
    );

    final oauthCredential = OAuthProvider('apple.com').credential(
      idToken: appleCredential.identityToken,
      rawNonce: rawNonce,
    );

    final result =
        await FirebaseAuth.instance.signInWithCredential(oauthCredential);

    // Apple só envia o nome no primeiro login
    if (appleCredential.givenName != null) {
      final displayName =
          '${appleCredential.givenName} ${appleCredential.familyName ?? ''}'
              .trim();
      await result.user?.updateDisplayName(displayName);
      await result.user?.getIdToken(true);
    }
  }

  Future<void> logout() async {
    await FirebaseAuth.instance.signOut();
  }

  void updateNome(String nome) {
    FirebaseAuth.instance.currentUser?.updateDisplayName(nome);
    notifyListeners();
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  String _generateNonce([int length = 32]) {
    const charset =
        '0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._';
    final random = Random.secure();
    return List.generate(
        length, (_) => charset[random.nextInt(charset.length)]).join();
  }

  String _sha256ofString(String input) {
    final bytes = utf8.encode(input);
    final digest = sha256.convert(bytes);
    return digest.toString();
  }
}
