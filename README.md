# Patryk_Chamera_Java_Krakow - Optymalizator Płatności Zamówień

## Autor

* **Imię i Nazwisko:** Patryk Chamera
* **GitHub:** [xhamera1](https://github.com/xhamera1)
* **LinkedIn:** [Patryk Chamera](https://www.linkedin.com/in/patryk-chamera-309835289/)
* **Email:** [chamerapatryk@gmail.com](mailto:chamerapatryk@gmail.com)
* **Projekt:** Patryk_Chamera_Java_Krakow (Zadanie rekrutacyjne Ocado Technology)

## Użyte Technologie i Biblioteki

* **Język programowania:** Java 21 
* **System budowania:** Apache Maven
* **Testowanie:**
    * JUnit 5 (Jupiter)
    * Mockito: 
* **Obsługa JSON:**
    * Jackson Databind
* **Narzędzia deweloperskie:**
    * Lombok
    * IntelliJ IDEA

## Opis Problemu

Aplikacja została stworzona w celu rozwiązania problemu optymalizacji metod płatności dla serii zamówień w sklepie internetowym. Głównym celem jest maksymalizacja łącznego rabatu uzyskanego przez klienta, przy jednoczesnym zapewnieniu, że wszystkie zamówienia zostaną w pełni opłacone. Algorytm uwzględnia różne rodzaje promocji:
* Rabaty procentowe za płatność całego zamówienia określoną kartą płatniczą (promocje specyficzne dla zamówienia).
* Rabat procentowy za opłacenie co najmniej 10% wartości zamówienia punktami lojalnościowymi (promocja uniwersalna, wykluczająca rabat kartą).
* Specjalny rabat procentowy za opłacenie całego zamówienia punktami lojalnościowymi.

Dodatkowym kryterium jest preferowanie płatności punktami lojalnościowymi, o ile nie zmniejsza to łącznego uzyskanego rabatu, aby minimalizować wydatki z kart płatniczych.

## Podejście do Rozwiązania

Zaimplementowane rozwiązanie opiera się na podejściu heurystycznym, które można określić jako algorytm zachłanny:

1.  **Sortowanie Zamówień:** Zamówienia są wstępnie sortowane w kolejności malejącej według maksymalnego teoretycznego rabatu, jaki można dla nich uzyskać. Celem tego kroku jest priorytetyzacja zamówień, które potencjalnie mogą przynieść największe oszczędności.
2.  **Generowanie Opcji Płatności:** Dla każdego zamówienia (w ustalonej kolejności) generowane są wszystkie możliwe i prawidłowe opcje płatności, biorąc pod uwagę aktualnie dostępne limity środków dla poszczególnych metod płatności oraz zasady promocji.
3.  **Wybór Najlepszej Opcji:** Spośród wygenerowanych opcji dla danego zamówienia, wybierana jest ta, która lokalnie maksymalizuje korzyść:
    * Najpierw maksymalizuje uzyskany rabat.
    * Następnie, w przypadku równego rabatu, maksymalizuje ilość użytych punktów lojalnościowych.
4.  **Aplikacja Płatności:** Po wyborze najlepszej opcji, odpowiednie kwoty są "pobierane" z limitów metod płatności, a suma wydatków dla każdej metody jest aktualizowana.

Proces ten jest powtarzany dla wszystkich zamówień.

### Ograniczenia Obecnego Podejścia

Zastosowany algorytm zachłanny, ze względu na swoją naturę podejmowania lokalnie optymalnych decyzji (najpierw sortowanie zamówień, a następnie wybór najlepszej opcji dla każdego z nich po kolei), nie gwarantuje znalezienia globalnego optimum dla całego zestawu zamówień. Oznacza to, że suma uzyskanych rabatów może być niższa niż maksymalna możliwa do osiągnięcia. Przykładowo, dla danych testowych dostarczonych w zadaniu, algorytm uzyskuje wynik:
`PUNKTY 100.00`, `BosBankrut 192.50`, `mZysk 170.00`,
który jest nieco mniej korzystny niż przykładowy wynik z PDF (`PUNKTY 100.00`, `BosBankrut 190.00`, `mZysk 165.00`).

Aby uzyskać rozwiązanie globalnie optymalne, problem ten mógłby zostać zamodelowany i rozwiązany przy użyciu technik programowania matematycznego, na przykład jako problem programowania liniowego całkowitoliczbowego (MILP - Mixed Integer Linear Programming) i rozwiązany przy pomocy dedykowanego solvera. Takie podejście pozwoliłoby na jednoczesne rozważenie wszystkich zamówień i metod płatności w celu znalezienia gwarantowanego optimum, jednak kosztem potencjalnie większej złożoności implementacji modelu i czasu obliczeń.

## Struktura Projektu

Główna struktura projektu prezentuje się następująco:

```
.
├── .idea/                  # Pliki konfiguracyjne IntelliJ IDEA
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/chamera/patryk/
│   │   │       ├── config/         # Walidacja danych wejściowych (InputValidator.java)
│   │   │       ├── exception/      # Niestandardowe klasy wyjątków
│   │   │       ├── model/          # Klasy POJO reprezentujące dane (Order.java, PaymentMethod.java)
│   │   │       ├── parser/         # Parsowanie plików JSON (JsonDataParser.java)
│   │   │       └── service/        # Główna logika biznesowa (PaymentOptimizerService.java)
│   │   │       ├── ApplicationRunner.java # Koordynator przepływu aplikacji
│   │   │       └── Main.java              # Główny punkt wejścia aplikacji
│   │   └── resources/          # Przykładowe pliki JSON (orders.json, paymentmethods.json)
│   └── test/
│       ├── java/
│       │   └── com/chamera/patryk/
│       │       ├── config/         # Testy dla InputValidator
│       │       ├── parser/         # Testy dla JsonDataParser
│       │       └── service/        # Testy dla PaymentOptimizerService
│       │       └── ApplicationRunnerTest.java # Testy dla ApplicationRunner (integracyjne)
│       └── resources/
│           ├── orders/           # Pliki JSON używane w testach parsera zamówień
│           └── paymentmethods/   # Pliki JSON używane w testach parsera metod płatności
├── target/                   # Katalog generowany przez Maven
│                               # Zawiera skompilowane klasy, raporty, zbudowany JAR itp.
├── app.jar                   # Zbudowany, wykonywalny plik aplikacji (fat-jar)
├── pom.xml                   # Plik konfiguracyjny Mavena 
└── README.md                 # Ten plik
```


## Uruchomienie Aplikacji

### Wymagania

* Java Development Kit (JDK) w wersji 21.
* Apache Maven (do samodzielnej budowy projektu ze źródeł).

### Dostarczony Plik JAR

W głównym katalogu projektu znajduje się gotowy do uruchomienia plik `app.jar`. Został on zbudowany przy użyciu Mavena i zawiera wszystkie niezbędne zależności (tzw. "fat-jar").

Jeśli chcesz samodzielnie zbudować projekt ze źródeł:
1.  **Sklonuj repozytorium:**
    ```bash
    git clone https://github.com/xhamera1/Patryk_Chamera_Java_Krakow.git
    cd Patryk_Chamera_Java_Krakow
    ```
    Lub rozpakuj dostarczone archiwum z kodem źródłowym i przejdź do głównego katalogu projektu.

2.  **Zbuduj projekt:**
    W głównym katalogu projektu uruchom komendę Mavena:
    ```bash
    mvn clean package
    ```
    Spowoduje to utworzenie pliku `app.jar` w katalogu `target/`.

### Uruchomienie

Aplikację uruchamia się z wiersza poleceń, podając ścieżki do plików JSON z zamówieniami i metodami płatności.

**A. Uruchomienie dostarczonego `app.jar` (z głównego katalogu projektu):**

Zakładając, że znajdujesz się w głównym katalogu projektu (`Patryk_Chamera_Java_Krakow`), gdzie umieszczony jest `app.jar`:

* **Użycie przykładowych plików JSON (dostarczonych w projekcie):**
    ```bash
    java -jar app.jar src/main/resources/orders.json src/main/resources/paymentmethods.json
    ```
* **Użycie własnych plików JSON:**
    ```bash
    java -jar app.jar /sciezka/do/twojego/orders.json /sciezka/do/twojego/paymentmethods.json
    ```

**B. Uruchomienie `app.jar` bezpośrednio z katalogu `target/` (po samodzielnej budowie):**

Jeśli samodzielnie zbudowałeś projekt i chcesz uruchomić plik JAR bezpośrednio z katalogu `target/` (bez jego kopiowania), pamiętaj, że ścieżki do plików zasobów (np. `src/main/resources/...`) muszą być odpowiednio dostosowane lub musisz użyć ścieżek bezwzględnych.

* **Przykład z plikami zasobów (zakładając, że jesteś w głównym katalogu projektu):**
    ```bash
    java -jar target/app.jar src/main/resources/orders.json src/main/resources/paymentmethods.json
    ```

* **Przykład z własnymi plikami (ścieżki bezwzględne):**
    ```bash
    java -jar target/app.jar /pelna/sciezka/do/orders.json /pelna/sciezka/do/paymentmethods.json
    ```

**Zalecane jest uruchamianie dostarczonego `app.jar` z głównego katalogu projektu.**

## Przykładowy wynik dla danych testowych z PDF (uzyskany przez ten algorytm):
```
PUNKTY 100.00
BosBankrut 192.50
mZysk 170.00
```

## Testy

Projekt zawiera zestaw testów jednostkowych oraz integracyjnych mających na celu weryfikację poprawności działania poszczególnych komponentów:

* **`InputValidatorTest.java`:** Sprawdza walidację argumentów i ścieżek plików.
* **`JsonDataParserTest.java`:** Weryfikuje parsowanie danych z plików JSON.
* **`PaymentOptimizerServiceTest.java`:** Pokrywa logikę obliczania rabatów, generowania opcji płatności i wyboru najlepszej opcji, w tym dla głównej metody `optimizePayments`.
* **`ApplicationRunnerTest.java`:** Testuje ogólny przepływ aplikacji, weryfikując interakcje między komponentami (np. walidatorem, parserem, serwisem optymalizującym) oraz obsługę różnych scenariuszy wejściowych i błędów na poziomie aplikacji. Te testy mają charakter bardziej integracyjny.

## Dokumentacja Kodu

Kod źródłowy, w szczególności kluczowe klasy i metody, został opatrzony komentarzami dokumentacyjnymi Javadoc.


## Licencja
Ten projekt jest udostępniany na licencji MIT [LICENSE](LICENSE)
