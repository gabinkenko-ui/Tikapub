import 'package:flutter_test/flutter_test.dart';

import 'package:tikapub_mobile/main.dart';

void main() {
  testWidgets('Home screen shows the three generator entry points', (WidgetTester tester) async {
    await tester.pumpWidget(const TikapubApp());

    expect(find.text('Tikapub'), findsOneWidget);
    expect(find.text('Citation'), findsOneWidget);
    expect(find.text('Voix off'), findsOneWidget);
    expect(find.text('Compilation'), findsOneWidget);
  });

  testWidgets('Tapping Citation navigates to the quote screen', (WidgetTester tester) async {
    await tester.pumpWidget(const TikapubApp());
    await tester.tap(find.text('Citation'));
    await tester.pumpAndSettle();

    expect(find.text('Texte de la citation'), findsOneWidget);
  });
}
