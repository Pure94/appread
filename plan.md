Technologie (Zaktualizowane):

UI Shell: Electron
UI Framework: Vue.js 3 + TypeScript (działające wewnątrz Electrona)
UI Stan: Pinia
UI Komunikacja: Axios (do lokalnego backendu) lub Electron IPC
Rdzeń Logiki (Lokalny Backend): Java, Spring Boot, Spring AI
Baza Wektorowa: PostgreSQL z rozszerzeniem PgVector
Model Embeddingów: Model Transformer (np. all-MiniLM-L6-v2, nomic-embed-text) hostowany lokalnie przez Ollama (lub inny serwer/API, np. Hugging Face Inference API - do ustalenia)
Model Generowania: Model LLM (np. Gemini, Llama 3 przez Ollama)
Obsługa Git: JGit
Diagramy: Mermaid.js (renderowane w Electron/Vue)
Lokalne Środowisko Dev: Docker Compose (PostgreSQL+PgVector, Ollama, Backend, Frontend)
Instalator: electron-builder / electron-forge
Bezpieczeństwo: Biblioteki do bezpiecznego przechowywania danych (np. keytar w Electron, API systemowe w Javie)
Faza 1: Setup i Podstawowa Struktura (Ok. 3-4 dni)

Projekt Backendu Java:
[ ] Stworzenie projektu Spring Boot.
[ ] Dodanie zależności: spring-boot-starter-web (dla localhost API), spring-boot-starter-data-jpa, spring-ai-core, spring-ai-ollama-starter (lub inny dla embeddingów/generacji), spring-ai-pgvector-store-starter, postgresql driver, com.pgvector:pgvector-java (jeśli potrzebny poza Spring AI), jgit, lombok.
[ ] Konfiguracja repozytorium Git dla backendu.
Projekt Frontend/Shell Electron:
[ ] Inicjalizacja projektu Electron + Vue 3 + TypeScript.
[ ] Dodanie zależności: axios, pinia, mermaid.
[ ] Podstawowa struktura okna i projektu Vue.
[ ] Konfiguracja repozytorium Git dla frontendu (lub monorepo).
Środowisko Lokalnego Rozwoju:
[ ] Stworzenie docker-compose.yml zawierającego:
Serwis PostgreSQL z włączonym rozszerzeniem PgVector (np. używając obrazu pgvector/pgvector).
Serwis Ollama (np. oficjalny obraz ollama/ollama) z pre-pobranym modelem embeddingów i modelem do generacji.
Serwis dla backendu Java.
Serwis dla frontendu Electron (opcjonalnie, można uruchamiać lokalnie).
[ ] Konfiguracja połączenia Backendu Java z PostgreSQL/PgVector i Ollama w application.properties/yml.
Komunikacja i Uruchamianie:
[ ] Ustalenie sposobu komunikacji Electron <-> Backend Java (localhost REST API wydaje się najprostsze).
[ ] Implementacja mechanizmu startu/stopu backendu Java przez Electron (lub zarządzanie przez Docker Compose lokalnie).
[ ] Prosty endpoint /health w backendzie i testowe wywołanie z Electrona.
Faza 2: Rdzeń Logiki Backendu Java - Przetwarzanie Danych i PgVector (Ok. 5-8 dni)

Obsługa Repozytoriów:
[ ] Implementacja GitService (używając JGit) do klonowania repozytoriów (publicznych/prywatnych) do lokalnego folderu tymczasowego.
[ ] Implementacja bezpiecznego przechowywania PAT (GitHub/GitLab tokens) lokalnie przy użyciu mechanizmów OS (np. Java KeyStore lub zewnętrzne biblioteki).
[ ] Implementacja listowania struktury plików/folderów w sklonowanym repo.
[ ] Implementacja GitAPIService do pobierania treści pojedynczych plików z API GitHub/GitLab (potrzebne np. dla kontekstu bieżącego pliku).
Przetwarzanie Dokumentów:
[ ] Implementacja DocumentProcessingService:
Odczytywanie plików z repozytorium (z filtrowaniem jak w deepwiki-open).
Dzielenie plików na mniejsze części (chunks) - użycie TokenTextSplitter z Spring AI lub innej logiki.
Oznaczanie metadanych dla chunków (ścieżka pliku, numery linii itp.).
Embeddingi i Zapis do PgVector:
[ ] Konfiguracja EmbeddingClient w Spring AI do użycia modelu Transformer przez Ollama.
[ ] Definicja encji JPA (@Entity) dla dokumentów/chunków, zawierającej pole typu vector (używając org.springframework.ai.vectorstore.PgVectorStore.PgVectorType lub com.pgvector.PGvector).
[ ] Implementacja EmbeddingService: Generowanie embeddingów dla chunków dokumentów za pomocą EmbeddingClient.
[ ] Implementacja PersistenceService: Zapisywanie chunków tekstu wraz z ich embeddingami do bazy PostgreSQL/PgVector używając Spring Data JPA i VectorStore ze Spring AI (PgVectorStore). Obsługa operacji wsadowych (batching).
Wyszukiwanie Wektorowe (Retrieval):
[ ] Implementacja RetrievalService:
Przyjmowanie zapytania użytkownika (query).
Generowanie embeddingu dla zapytania (używając tego samego EmbeddingClient).
Wykonywanie zapytania do PgVectorStore (np. similaritySearch) w celu znalezienia najbardziej relevantnych chunków z bazy.
Faza 3: Integracja AI - RAG i Generowanie Odpowiedzi (Ok. 4-7 dni)

Konfiguracja Modeli AI:
[ ] Konfiguracja ChatClient w Spring AI do użycia modelu generującego (np. Gemini przez Vertex AI, Llama 3 przez Ollama).
Implementacja RAG:
[ ] Implementacja RAGService:
Orkiestracja procesu: query -> RetrievalService -> pobranie relevantnych chunków -> budowanie promptu.
Prompt Engineering: Stworzenie szablonu promptu (inspirując się deepwiki-open), który zawiera: instrukcje systemowe, historię konwersacji, pobrany kontekst (chunki z PgVector), opcjonalnie zawartość aktualnie przeglądanego pliku (z GitAPIService), oraz zapytanie użytkownika.
Wywołanie ChatClient z przygotowanym promptem.
Przetwarzanie odpowiedzi (np. obsługa strumieniowania, formatowanie).
[ ] Implementacja ConversationMemoryService do zarządzania historią dialogu (prosta implementacja w pamięci lub zapis w bazie danych).
Obsługa Strumieniowania:
[ ] Skonfigurowanie ChatClient do pracy w trybie strumieniowym (jeśli model wspiera).
[ ] Implementacja endpointu API w backendzie, który zwraca odpowiedź jako Flux<String> lub podobny strumień Server-Sent Events (SSE).
Błędy i Fallbacki:
[ ] Implementacja obsługi błędów z API modeli AI (limity tokenów, błędy sieciowe).
[ ] Implementacja logiki fallback (np. ponowienie zapytania bez kontekstu RAG, jeśli wystąpi błąd limitu tokenów - wzorując się na deepwiki-open).
Faza 4: Generowanie Diagramów Mermaid (Ok. 2-4 dni)

[Zadania bez zmian w stosunku do poprzedniego planu desktopowego]
[ ] Zdecydowanie o strategii generowania składni Mermaid (AI vs. parsowanie kodu).
[ ] Implementacja logiki backendowej generującej składnię Mermaid.
[ ] Endpoint API/Handler IPC zwracający składnię Mermaid.
[ ] Frontend: Komponent Vue do renderowania Mermaid.js.
Faza 5: Budowa Interfejsu Użytkownika (Electron/Vue.js) (Ok. 5-8 dni)

[Zadania głównie bez zmian, z uwzględnieniem strumieniowania]
[ ] Projekt UI (sidebar, content, input).
[ ] Implementacja komponentów Vue (RepoInput, FileTree, ContentView, DiagramView, komponenty statusu/postępu).
[ ] Integracja renderowania Markdown i Mermaid.js.
[ ] Logika Frontendowa:
Wywołania API do lokalnego backendu Java.
Obsługa odpowiedzi strumieniowych z backendu dla generowanych odpowiedzi AI i wyświetlanie ich w czasie rzeczywistym.
Zarządzanie stanem (Pinia).
Obsługa ładowania i błędów.
Integracja z Electron API (np. keytar do bezpiecznego zarządzania PAT).
Faza 6: Integracja, Workflow i Ulepszenia (Ok. 3-5 dni)

Orkiestracja Workflow:
[ ] Połączenie wszystkich serwisów backendu w spójny przepływ: wpisanie URL -> klonowanie -> przetwarzanie i embedding -> RAG -> generowanie odpowiedzi/wiki.
[ ] Implementacja endpointu API/handlera IPC, który inicjuje pełną analizę repozytorium.
Struktura Wiki:
[ ] Zdefiniowanie, jak generowane treści i diagramy mają być prezentowane w formie wiki (strona główna, strony per plik/folder).
[ ] Endpointy API/handlery IPC dostarczające dane dla nawigacji i treści stron wiki.
Testowanie i Refinement:
[ ] Testy end-to-end kluczowych przepływów.
[ ] Optymalizacja zapytań PgVector.
[ ] Tuning promptów AI.
[ ] Ulepszenia UI/UX.
Faza 7: Pakowanie, Dystrybucja i Konserwacja (Ciągłe)

Pakowanie Aplikacji:
[ ] Konfiguracja electron-builder/electron-forge do tworzenia instalatorów (Windows, macOS, Linux).
[ ] Rozwiązanie kwestii zależności PostgreSQL+PgVector i Ollama:
Opcja 1 (Zalecana dla prostoty): Wymaganie od użytkownika posiadania Docker Desktop i dostarczenie docker-compose.yml do uruchomienia bazy i Ollamy. Aplikacja Electron łączyłaby się z kontenerami.
Opcja 2: Wymaganie od użytkownika ręcznej instalacji PostgreSQL (+PgVector) i Ollama.
Opcja 3 (Złożona): Próba dołączenia uproszczonej bazy (np. H2 z rozszerzeniem wektorowym?) i zarządzanie procesem Ollama bezpośrednio (trudniejsze).
[ ] Stworzenie czytelnej dokumentacji instalacji i konfiguracji dla użytkownika końcowego.
[ ] Podpisywanie kodu.
[ ] Mechanizm auto-aktualizacji (np. electron-updater).
Konserwacja:
[ ] Aktualizacje zależności, obsługa błędów, rozwój.