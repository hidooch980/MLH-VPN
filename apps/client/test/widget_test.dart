import 'package:flutter_test/flutter_test.dart';
import 'package:molla_vpn/main.dart';

void main() {
  testWidgets('shows honest disconnected bootstrap state', (tester) async {
    await tester.pumpWidget(const MollaVpnApp());

    expect(find.text('Molla VPN'), findsOneWidget);
    expect(find.text('Not connected'), findsOneWidget);
    expect(find.text('VPN engine is not configured yet'), findsOneWidget);
    expect(find.text('Connection unavailable in this build'), findsOneWidget);
  });
}
