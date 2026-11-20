# SMOQ Bonus Clicker

Projekt Android Studio aplikacji automatycznie naciskającej przycisk `OK` na ekranie bonusu SMOQ Games 27.

## Jak działa
- Usługa dostępności wykonuje zrzut ekranu co ok. 1,5 sekundy, gdy automat jest włączony.
- Google ML Kit OCR rozpoznaje tekst lokalnie na telefonie.
- Kliknięcie jest wykonywane tylko wtedy, gdy na ekranie występuje `BONUS` oraz osobny element tekstowy `OK`.
- Pozycja przycisku nie jest na stałe ustawiona, więc lewa/prawa strona nie ma znaczenia.
- Przycisk `IR A TUS CARTAS` nie jest wybierany.

## Budowanie APK przez GitHub Actions
W repozytorium GitHub:
1. Utwórz nowe, puste repozytorium.
2. Wgraj zawartość tego folderu (razem z ukrytym folderem `.github`).
3. Otwórz zakładkę **Actions** i uruchom workflow **Build Android APK** (albo poczekaj na automatyczny build po pushu do `main`).
4. Otwórz zakończony workflow i pobierz artefakt `SMOQ-Bonus-Clicker-APK`.
5. W ZIP-ie znajdziesz `app-debug.apk`.

## Pierwsze uruchomienie
1. Zainstaluj APK na Androidzie 11 lub nowszym.
2. Otwórz aplikację i włącz usługę w ustawieniach dostępności Androida.
3. Wróć do aplikacji, naciśnij START i otwórz grę.
4. STOP wyłącza automatyczne skanowanie.

## Ograniczenia i prywatność
- Wymaga Androida 11/API 30 lub nowszego, ponieważ korzysta z funkcji zrzutu ekranu usługi dostępności.
- Android wymaga ręcznego włączenia usługi dostępności.
- OCR może nie rozpoznać napisu w każdej rozdzielczości lub przy każdym skalowaniu. Przed dłuższym użyciem sprawdź działanie na ekranie bonusu.
- Zrzuty ekranu są analizowane lokalnie przez ML Kit; aplikacja nie wysyła ich na serwer.
- To nie jest jeszcze przetestowany build na Twoim telefonie. Jeśli układ lub napisy w grze różnią się od przesłanego zrzutu, może być potrzebna korekta.
