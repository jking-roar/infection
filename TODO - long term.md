# Funfection — Development Checklist

## Foundation
- [ ] Add Room database (or SharedPreferences) so the virus collection survives app restarts
- [ ] Create a `Player` entity: username, avatar seed, total infections count, join date
- [ ] Add `ScrollView` to `my_virus.xml` so content does not clip on small screens

---

## Sharing & Social

- [ ] **NFC tap-to-share** — Use `NfcAdapter` + NDEF to beam a virus share code when two phones tap; declare `android.permission.NFC`; fall back gracefully if NFC unavailable
- [ ] **QR code generation** — Generate a scannable QR from `virus.toShareCode()` using ZXing; show full-screen for a friend to scan
- [ ] **QR code scanning** — Add a scan button that opens the camera and reads a friend's QR → auto-populates the `friendCode` field
- [ ] **Deep link invite codes** — Register `funfection://virus/<code>` so shared links open the app and pre-load the friend virus
- [ ] **Combined board sharing** — Export an entire collection as a multi-virus blob so friends can import and infect against a whole lab

---

## Game Mechanics & Boards

- [ ] **Infection board / spread map** — Grid or hex map representing a city; tap to place virus; spreads to adjacent cells based on Infectivity; Resilience sets lifespan; Chaos randomly skips or double-spreads
- [ ] **Turn-based outbreak rounds** — "Simulate Outbreak" button runs N rounds and shows final board coverage %
- [ ] **Board merging** — Two players export boards; importing a friend's board overlaps both spreads and `InfectionEngine.infect()` produces a chimera at contested cells
- [ ] **Mutation events** — Random in-game events during simulation (e.g., "Climate shift: Resilience −1 for all viruses this round")
- [ ] **Virus evolution tree** — Track parent → child lineage so players can view a genealogy of all chimeras

---

## Achievements

- [ ] Create `Achievement` model: id, title, description, icon, unlocked boolean, unlock date
- [ ] **Patient Zero** — Create your first virus
- [ ] **Typhoid Tyler** — Infect 10 times
- [ ] **Outbreak** — Produce a virus with `OUTBREAK` infection rate
- [ ] **Chain Reaction** — Create a 3rd-generation chimera (child of two chimeras)
- [ ] **Going Viral** — Share a virus code that gets imported by another player (tracked via deep link callback)
- [ ] **NFC Carrier** — Infect someone via NFC tap
- [ ] **Full Spectrum** — Own at least one virus of every family: Spark, Echo, Mirth, Glitch, Bloom, Pulse
- [ ] **Chaos Agent** — Create a virus with Chaos score = 10
- [ ] **Iron Strain** — Create a virus with Resilience = 10
- [ ] **Speed Demon** — Create a virus with Infectivity = 10
- [ ] **Achievement screen** — Grid of locked/unlocked achievement cards with progress indicators

---

## Scoreboard / Leaderboard

- [ ] **Local high-score table** — Persist top outbreaks (player name, virus name, board coverage %, date) in Room
- [ ] **Lab Rating score** — Sum of all virus infection rates + mutation bonuses + chimera depth bonus; display prominently on main screen
- [ ] **Weekly challenge** — Rotating seed generates a challenge virus; players compete to produce the highest-stat chimera by week's end
- [ ] **Global leaderboard** *(optional)* — Firebase Firestore or REST backend; post best outbreak score with virus genome as proof; requires `INTERNET` permission and a backend

---

## Polish & Engagement

- [ ] **Onboarding flow** — First-run walkthrough: name your player, explain stats, create first virus with guided tap sequence
- [ ] **Virus card UI** — Replace `ListView` with `RecyclerView` + custom cards showing family color, genome badge, infection rate chip
- [ ] **Animated infection simulation** — Board cells animate with a pulse/wave as the virus spreads (ValueAnimator or Lottie)
- [ ] **Sound effects** — Bubbling sound on successful infection, fanfare on OUTBREAK rating, tap sound on NFC share
- [ ] **"Your virus is spreading" notification** — WorkManager scheduled notification 24 h after last session reminding the player to check their board
- [ ] **Dark mode polish** — Add proper `values-night/colors.xml` (currently the night folder is empty)
- [ ] **Virus nickname / notes** — Let the player rename a virus and add a short note to remember friend-gifted strains
- [ ] **Collection stats dashboard** — Summary card at top of lab: most infectious virus, rarest family, highest chaos, total chimeras created

---

## Technical / Infrastructure

- [ ] Runtime permission handling for NFC and camera (Android 6+)
- [ ] Extend JUnit suite to cover infection board spread simulation logic
- [ ] Add Proguard / R8 keep rules for `Virus` (it is `Serializable` and passed via intents)
- [ ] Plan Room migration scripts before first release so save data survives APK updates

---

## Suggested Priority Order

| Sprint | Items |
|--------|-------|
| Week 1 | Persistence → Player model → RecyclerView cards → Infection board → Outbreak simulation |
| Week 2 | NFC → QR generate → QR scan → Board sharing → Achievements (model + first 5) |
| Week 3 | Board merging → Evolution tree → Local leaderboard → Lab Rating → Onboarding |
| Week 4+ | Deep links → Global leaderboard → Weekly challenge → Animations → Sound → Polish pass |
