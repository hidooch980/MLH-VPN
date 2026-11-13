import 'package:material_ui/material_ui.dart';

void main() {
  runApp(const MollaVpnApp());
}

class MollaVpnApp extends StatelessWidget {
  const MollaVpnApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Molla VPN',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF635BFF),
          brightness: Brightness.dark,
        ),
        useMaterial3: true,
      ),
      home: const ConnectionHomePage(),
    );
  }
}

class ConnectionHomePage extends StatelessWidget {
  const ConnectionHomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 520),
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.shield_outlined, size: 72),
                  const SizedBox(height: 20),
                  Text(
                    'Molla VPN',
                    style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Your connection, under your control.',
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.bodyLarge,
                  ),
                  const SizedBox(height: 36),
                  const Card(
                    child: ListTile(
                      leading: Icon(Icons.circle, color: Colors.orange, size: 14),
                      title: Text('Not connected'),
                      subtitle: Text('VPN engine is not configured yet'),
                    ),
                  ),
                  const SizedBox(height: 16),
                  const FilledButton(
                    onPressed: null,
                    child: Text('Connection unavailable in this build'),
                  ),
                  const SizedBox(height: 20),
                  Text(
                    'Development preview • 0.1.0',
                    style: Theme.of(context).textTheme.labelSmall,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
