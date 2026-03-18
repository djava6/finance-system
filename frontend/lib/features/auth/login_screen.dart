import 'dart:io';
import 'package:firebase_analytics/firebase_analytics.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/providers/auth_provider.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  bool _loadingGoogle = false;
  bool _loadingApple = false;

  Future<void> _loginWithGoogle() async {
    setState(() => _loadingGoogle = true);
    try {
      await context.read<AuthProvider>().signInWithGoogle();
      await FirebaseAnalytics.instance.logEvent(name: 'login_google');
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
        );
      }
    } finally {
      if (mounted) setState(() => _loadingGoogle = false);
    }
  }

  Future<void> _loginWithApple() async {
    setState(() => _loadingApple = true);
    try {
      await context.read<AuthProvider>().signInWithApple();
      await FirebaseAnalytics.instance.logEvent(name: 'login_apple');
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
        );
      }
    } finally {
      if (mounted) setState(() => _loadingApple = false);
    }
  }

  bool get _showApple => !kIsWeb && (Platform.isIOS || Platform.isMacOS);

  @override
  Widget build(BuildContext context) {
    final loading = _loadingGoogle || _loadingApple;

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Icon(Icons.account_balance_wallet,
                    size: 72, color: Colors.green),
                const SizedBox(height: 16),
                Text('Finance System',
                    style: Theme.of(context).textTheme.headlineSmall,
                    textAlign: TextAlign.center),
                const SizedBox(height: 8),
                Text('Gerencie suas finanças com segurança',
                    style: Theme.of(context)
                        .textTheme
                        .bodyMedium
                        ?.copyWith(color: Colors.grey),
                    textAlign: TextAlign.center),
                const SizedBox(height: 48),
                FilledButton.icon(
                  onPressed: loading ? null : _loginWithGoogle,
                  icon: _loadingGoogle
                      ? const SizedBox(
                          height: 18,
                          width: 18,
                          child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.login),
                  label: const Text('Entrar com Google'),
                ),
                if (_showApple) ...[
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    style: FilledButton.styleFrom(
                      backgroundColor: Colors.black,
                      foregroundColor: Colors.white,
                    ),
                    onPressed: loading ? null : _loginWithApple,
                    icon: _loadingApple
                        ? const SizedBox(
                            height: 18,
                            width: 18,
                            child: CircularProgressIndicator(
                                strokeWidth: 2, color: Colors.white))
                        : const Icon(Icons.apple),
                    label: const Text('Entrar com Apple'),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
