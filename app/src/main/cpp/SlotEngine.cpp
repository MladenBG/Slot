// ─────────────────────────────────────────────────────────────────────────────
//  SlotEngine.cpp  –  MagicsSlot core game logic
// ─────────────────────────────────────────────────────────────────────────────
#include "SlotEngine.h"
#include <android/log.h>
#include <algorithm>
#include <cmath>

#define LOG_TAG "MagicsSlot"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace MagicsSlot {

// ── 20 payline definitions (row per reel, 0=top 1=mid 2=bot) ─────────────────
static const std::array<std::array<int, MAX_REEL_COUNT>, PAYLINE_COUNT> PAYLINES = {{
    {1,1,1,1,1}, {0,0,0,0,0}, {2,2,2,2,2}, {0,1,2,1,0}, {2,1,0,1,2},
    {0,0,1,2,2}, {2,2,1,0,0}, {1,0,0,0,1}, {1,2,2,2,1}, {0,1,1,1,0},
    {2,1,1,1,2}, {1,0,1,0,1}, {1,2,1,2,1}, {0,0,1,0,0}, {2,2,1,2,2},
    {0,1,0,1,0}, {2,1,2,1,2}, {1,1,0,1,1}, {1,1,2,1,1}, {0,2,0,2,0},
}};

// ── Pay table [symbol][count-3] ───────────────────────────────────────────────
static const float PAY_TABLE[SYM_COUNT][3] = {
    {  5.f, 25.f,100.f}, // WILD
    {  3.f, 15.f, 50.f}, // SCATTER
    { 10.f, 50.f,200.f}, // SEVEN (jackpot)
    {  5.f, 20.f, 75.f}, // BAR3
    {  3.f, 12.f, 40.f}, // BAR2
    {  2.f,  8.f, 25.f}, // BAR1
    {  4.f, 15.f, 50.f}, // DIAMOND
    {  1.f,  5.f, 15.f}, // CHERRY
};

// ── Casino Grade Reel Strips ──────────────────────────────────────────────────
static const Symbol REEL_STRIPS[MAX_REEL_COUNT][REEL_STRIP] = {
    // Reel 1: High hit frequency, lots of low symbols (Cherries, Bars)
    { SYM_CHERRY, SYM_BAR1, SYM_CHERRY, SYM_DIAMOND, SYM_CHERRY, SYM_BAR2, SYM_CHERRY, SYM_BAR1,
      SYM_WILD, SYM_CHERRY, SYM_BAR3, SYM_CHERRY, SYM_SEVEN, SYM_BAR1, SYM_CHERRY, SYM_BAR2,
      SYM_SCATTER, SYM_CHERRY, SYM_DIAMOND, SYM_BAR1, SYM_CHERRY, SYM_BAR3, SYM_CHERRY, SYM_BAR2,
      SYM_CHERRY, SYM_BAR1, SYM_DIAMOND, SYM_CHERRY, SYM_WILD, SYM_BAR2, SYM_CHERRY, SYM_BAR1,
      SYM_CHERRY, SYM_SEVEN, SYM_BAR3, SYM_CHERRY, SYM_DIAMOND, SYM_BAR1, SYM_CHERRY, SYM_BAR2 },

    // Reel 2: Medium frequency
    { SYM_BAR1, SYM_CHERRY, SYM_BAR2, SYM_DIAMOND, SYM_BAR1, SYM_CHERRY, SYM_BAR3, SYM_SEVEN,
      SYM_BAR2, SYM_CHERRY, SYM_BAR1, SYM_DIAMOND, SYM_BAR2, SYM_CHERRY, SYM_BAR3, SYM_WILD,
      SYM_BAR1, SYM_SCATTER, SYM_BAR2, SYM_CHERRY, SYM_BAR1, SYM_DIAMOND, SYM_BAR2, SYM_CHERRY,
      SYM_BAR3, SYM_SEVEN, SYM_BAR1, SYM_CHERRY, SYM_BAR2, SYM_DIAMOND, SYM_BAR1, SYM_CHERRY,
      SYM_BAR3, SYM_WILD, SYM_BAR2, SYM_CHERRY, SYM_BAR1, SYM_DIAMOND, SYM_BAR2, SYM_CHERRY },

    // Reel 3: Balanced, some Wilds
    { SYM_DIAMOND, SYM_BAR2, SYM_SEVEN, SYM_BAR1, SYM_WILD, SYM_BAR3, SYM_CHERRY, SYM_DIAMOND,
      SYM_BAR2, SYM_SCATTER, SYM_BAR1, SYM_CHERRY, SYM_DIAMOND, SYM_BAR3, SYM_SEVEN, SYM_BAR2,
      SYM_WILD, SYM_BAR1, SYM_CHERRY, SYM_DIAMOND, SYM_BAR2, SYM_BAR3, SYM_SEVEN, SYM_BAR1,
      SYM_CHERRY, SYM_DIAMOND, SYM_BAR2, SYM_WILD, SYM_BAR1, SYM_CHERRY, SYM_DIAMOND, SYM_BAR3,
      SYM_SEVEN, SYM_BAR2, SYM_SCATTER, SYM_BAR1, SYM_CHERRY, SYM_DIAMOND, SYM_BAR2, SYM_BAR3 },

    // Reel 4: Lower hit frequency, suspense
    { SYM_SEVEN, SYM_BAR3, SYM_DIAMOND, SYM_BAR2, SYM_CHERRY, SYM_BAR1, SYM_SEVEN, SYM_BAR3,
      SYM_WILD, SYM_DIAMOND, SYM_BAR2, SYM_CHERRY, SYM_BAR1, SYM_SEVEN, SYM_BAR3, SYM_DIAMOND,
      SYM_SCATTER, SYM_BAR2, SYM_CHERRY, SYM_BAR1, SYM_SEVEN, SYM_BAR3, SYM_WILD, SYM_DIAMOND,
      SYM_BAR2, SYM_CHERRY, SYM_BAR1, SYM_SEVEN, SYM_BAR3, SYM_DIAMOND, SYM_BAR2, SYM_CHERRY,
      SYM_BAR1, SYM_SEVEN, SYM_BAR3, SYM_DIAMOND, SYM_WILD, SYM_BAR2, SYM_CHERRY, SYM_BAR1 },

    // Reel 5: Hardest to hit the big symbols (near miss effect)
    { SYM_BAR3, SYM_SEVEN, SYM_BAR2, SYM_DIAMOND, SYM_BAR1, SYM_CHERRY, SYM_BAR3, SYM_SEVEN,
      SYM_BAR2, SYM_DIAMOND, SYM_BAR1, SYM_CHERRY, SYM_BAR3, SYM_WILD, SYM_BAR2, SYM_DIAMOND,
      SYM_BAR1, SYM_CHERRY, SYM_BAR3, SYM_SEVEN, SYM_BAR2, SYM_SCATTER, SYM_BAR1, SYM_CHERRY,
      SYM_BAR3, SYM_SEVEN, SYM_BAR2, SYM_DIAMOND, SYM_BAR1, SYM_CHERRY, SYM_BAR3, SYM_WILD,
      SYM_BAR2, SYM_DIAMOND, SYM_BAR1, SYM_CHERRY, SYM_BAR3, SYM_SEVEN, SYM_BAR2, SYM_DIAMOND }
};

SlotEngine::SlotEngine(int slotType)
    : m_rng(std::random_device{}())
    , m_dist(0, REEL_STRIP - 1)
    , m_balance(100.0) // Start balance at 100 as requested
    , m_betPerLine(1.0)
    , m_spinning(false)
    , m_freeSpinsLeft(0)
    , m_spinTimer(0.f)
    , m_slotType(slotType)
    , m_reelCount(slotType == 1 ? 3 : MAX_REEL_COUNT)
{
    initStrips();
    for (auto& rs : m_reelState) rs = {0.f, 0.f, false, 0, 0.f};
    m_lastResult = {};
    
    // Generate initial random stop positions so the screen is populated with a nice mix of symbols at start
    generateStops();
    for (int r = 0; r < m_reelCount; ++r) {
        m_reelState[r].offset = static_cast<float>(m_stopPositions[r]);
    }
    buildGrid();
    
    LOGI("SlotEngine ready. Balance=%.2f", m_balance);
}

void SlotEngine::initStrips() {
    for (int r = 0; r < m_reelCount; ++r) {
        std::copy(std::begin(REEL_STRIPS[r]), std::end(REEL_STRIPS[r]), std::begin(m_strips[r]));
    }
}

void SlotEngine::setBalance(double c) { m_balance   = c; }
void SlotEngine::setBet(double b)     { m_betPerLine = std::clamp(b, 0.05, 5000.0); }

void SlotEngine::startSpin() {
    if (!canSpin()) return;
    if (!inFreeSpins()) m_balance -= getTotalBet();
    else                --m_freeSpinsLeft;

    m_spinning = false; // We resolve instantly
    m_spinTimer = 0.f;
    generateStops();

    for (int r = 0; r < m_reelCount; ++r) {
        m_reelState[r].offset = static_cast<float>(m_stopPositions[r]);
        m_reelState[r].velocity = 0.f;
        m_reelState[r].spinning = false;
    }
    
    buildGrid();
    evaluatePaylines();
    m_balance += m_lastResult.totalWin;
    if (m_lastResult.freeSpins > 0) m_freeSpinsLeft += m_lastResult.freeSpins;

    LOGI("Spin finished instantly. Win=%.2f Balance=%.2f", m_lastResult.totalWin, m_balance);
}

void SlotEngine::generateStops() {
    for (int r = 0; r < m_reelCount; ++r)
        m_stopPositions[r] = m_dist(m_rng);
}

void SlotEngine::stopReel(int r) {
    if (r < 0 || r >= m_reelCount) return;
    m_reelState[r].spinning = false;
}
void SlotEngine::stopAllReels() {
    for (int r = 0; r < m_reelCount; ++r) stopReel(r);
}

void SlotEngine::update(float dt) {
    if (!m_spinning) return;
    
    m_spinTimer += dt;
    
    // Staggered stop: Reel 0 stops after 1.0s, and subsequent reels stop in 0.3s intervals
    for (int r = 0; r < m_reelCount; ++r) {
        if (m_reelState[r].spinning && m_spinTimer > 1.0f + r * 0.3f) {
            stopReel(r);
        }
    }

    for (int r = 0; r < m_reelCount; ++r) updateReelPhysics(r, dt);

    if (isSettled()) {
        m_spinning = false;
        buildGrid();
        evaluatePaylines();
        m_balance += m_lastResult.totalWin;
        if (m_lastResult.freeSpins > 0) m_freeSpinsLeft += m_lastResult.freeSpins;
        LOGI("Spin done. Win=%.2f Balance=%.2f", m_lastResult.totalWin, m_balance);
        if (onSpinComplete) onSpinComplete(m_lastResult);
    }
}

void SlotEngine::updateReelPhysics(int r, float dt) {
    auto& rs = m_reelState[r];
    if (!rs.spinning && reelSettled(r)) return;

    if (rs.spinning) {
        rs.offset += rs.velocity * dt;
        while (rs.offset >= REEL_STRIP) rs.offset -= REEL_STRIP;
    } else {
        float target = static_cast<float>(m_stopPositions[r]);
        float dist   = target - rs.offset;
        while (dist < 0)          dist += REEL_STRIP;
        while (dist > REEL_STRIP) dist -= REEL_STRIP;

        if (rs.velocity > 200.f) {
            rs.velocity -= 4000.f * dt;
            rs.offset   += rs.velocity * dt;
            while (rs.offset >= REEL_STRIP) rs.offset -= REEL_STRIP;
        } else {
            if (dist < 0.1f || dist > REEL_STRIP - 0.1f) {
                rs.offset = target;
                rs.velocity = 0.f;
            } else {
                float step = std::min(dist, 600.f * dt);
                rs.offset += step;
                while (rs.offset >= REEL_STRIP) rs.offset -= REEL_STRIP;
            }
        }
    }
}

bool SlotEngine::reelSettled(int r) const {
    return !m_reelState[r].spinning && m_reelState[r].velocity < 1.f;
}
bool SlotEngine::isSettled() const {
    for (int r = 0; r < m_reelCount; ++r) if (!reelSettled(r)) return false;
    return true;
}

void SlotEngine::buildGrid() {
    for (int r = 0; r < m_reelCount; ++r) {
        int base = static_cast<int>(m_reelState[r].offset) % REEL_STRIP;
        for (int row = 0; row < ROW_COUNT; ++row)
            m_lastResult.grid[r][row] = m_strips[r][(base + row) % REEL_STRIP];
    }
}

void SlotEngine::evaluatePaylines() {
    m_lastResult.wins.clear();
    m_lastResult.totalWin   = 0.f;
    m_lastResult.scatterWin = false;
    m_lastResult.freeSpins  = 0;
    m_lastResult.jackpot    = false;

    int scatters = 0;
    for (int r = 0; r < m_reelCount; ++r)
        for (int row = 0; row < ROW_COUNT; ++row)
            if (m_lastResult.grid[r][row] == SYM_SCATTER) ++scatters;

    if (scatters >= 3) {
        m_lastResult.scatterWin = true;
        m_lastResult.freeSpins  = scatters * 3;
        m_lastResult.totalWin  += static_cast<float>(scatters - 2) * 5.f * m_betPerLine;
    }

    for (int p = 0; p < PAYLINE_COUNT; ++p) {
        const auto& line = PAYLINES[p];
        Symbol first = m_lastResult.grid[0][line[0]];
        Symbol matchSym = first;
        int count = 1;

        for (int r = 1; r < m_reelCount; ++r) {
            Symbol sym   = m_lastResult.grid[r][line[r]];
            bool isWild  = (sym == SYM_WILD);
            bool matchWild = (matchSym == SYM_WILD);
            if (sym == matchSym || isWild || matchWild) {
                if (matchWild && !isWild) matchSym = sym;
                ++count;
            } else break;
        }

        if (count >= 3 && matchSym != SYM_SCATTER) {
            float mult = symbolMultiplier(matchSym, count);
            float win  = mult * m_betPerLine;
            m_lastResult.wins.push_back({p, matchSym, count, mult, first == SYM_WILD});
            m_lastResult.totalWin += win;
            if (matchSym == SYM_SEVEN && count == m_reelCount) {
                m_lastResult.jackpot   = true;
                m_lastResult.totalWin += (m_reelCount == 3 ? 1000.f : 5000.f) * m_betPerLine;
                LOGI("JACKPOT!");
            }
        }
    }
}

float SlotEngine::symbolMultiplier(Symbol sym, int count) const {
    if (sym >= SYM_COUNT) return 0.f;
    return PAY_TABLE[sym][std::clamp(count - 3, 0, 2)];
}

Symbol SlotEngine::getGridSymbol(int reel, int row) const {
    int base = static_cast<int>(m_reelState[reel].offset) % REEL_STRIP;
    int idx = (base + row) % REEL_STRIP;
    if (idx < 0) idx += REEL_STRIP;
    return m_strips[reel][idx];
}

} // namespace MagicsSlot
