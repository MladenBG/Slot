#pragma once
// ─────────────────────────────────────────────────────────────────────────────
//  SlotEngine.h  –  Core game logic: RNG, reels, paylines, balance
// ─────────────────────────────────────────────────────────────────────────────
#include <array>
#include <vector>
#include <random>
#include <cstdint>
#include <functional>

namespace MagicsSlot {

enum Symbol : uint8_t {
    SYM_WILD    = 0,
    SYM_SCATTER = 1,
    SYM_SEVEN   = 2,
    SYM_BAR3    = 3,
    SYM_BAR2    = 4,
    SYM_BAR1    = 5,
    SYM_DIAMOND = 6,
    SYM_CHERRY  = 7,
    SYM_COUNT   = 8
};

static constexpr int MAX_REEL_COUNT = 5;
static constexpr int ROW_COUNT     = 3;
static constexpr int PAYLINE_COUNT = 20;
static constexpr int REEL_STRIP    = 40;

struct WinResult {
    int    payline;
    Symbol symbol;
    int    count;
    float  multiplier;
    bool   isWild;
};

struct SpinResult {
    std::array<std::array<Symbol, ROW_COUNT>, MAX_REEL_COUNT> grid;
    std::vector<WinResult> wins;
    float totalWin;
    bool  scatterWin;
    int   freeSpins;
    bool  jackpot;
};

struct ReelState {
    float offset;
    float velocity;
    bool  spinning;
    int   targetStop;
    float bouncePhase;
};

class SlotEngine {
public:
    SlotEngine(int slotType = 0);

    int    getReelCount() const { return m_reelCount; }

    void   setBalance(double credits);
    void   setBet(double betPerLine);
    double getBalance() const { return m_balance; }
    double getBet()     const { return m_betPerLine; }
    double getTotalBet()const { return m_betPerLine * PAYLINE_COUNT; }
    bool   canSpin()    const { return m_balance >= getTotalBet() && !m_spinning; }

    void startSpin();
    void stopReel(int reelIndex);
    void stopAllReels();
    bool isSpinning() const { return m_spinning; }
    bool isSettled()  const;

    void update(float dt);

    const SpinResult& getLastResult() const { return m_lastResult; }
    const ReelState&  getReelState(int r) const { return m_reelState[r]; }

    bool inFreeSpins()   const { return m_freeSpinsLeft > 0; }
    int  freeSpinsLeft() const { return m_freeSpinsLeft; }

    Symbol getGridSymbol(int reel, int row) const;

    std::function<void(const SpinResult&)> onSpinComplete;

private:
    std::mt19937                    m_rng;
    std::uniform_int_distribution<> m_dist;

    double m_balance;
    double m_betPerLine;
    bool   m_spinning;
    int    m_freeSpinsLeft;
    float  m_spinTimer;

    int            m_slotType;
    int            m_reelCount;
    Symbol         m_strips[MAX_REEL_COUNT][REEL_STRIP];
    ReelState      m_reelState[MAX_REEL_COUNT];
    int            m_stopPositions[MAX_REEL_COUNT];
    SpinResult                         m_lastResult;

    void  initStrips();
    void  generateStops();
    void  buildGrid();
    void  evaluatePaylines();
    float symbolMultiplier(Symbol sym, int count) const;
    void  updateReelPhysics(int r, float dt);
    bool  reelSettled(int r) const;
};

} // namespace MagicsSlot
