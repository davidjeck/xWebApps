
// class PokerCard and class PokerRank were tranlated from Java version by AI.
// The translations have not been well-tested.  You can do whatever you want with
// this code, at your own risk.

/**
 * An object of type PokerCard represents a playing card from a
 * standard Poker deck, including Jokers.
 */
class PokerCard {
    // Codes for the 4 suits, plus Joker.
    static SPADES = 0;
    static HEARTS = 1;
    static DIAMONDS = 2;
    static CLUBS = 3;
    static JOKER = 4;

    // Codes for the non-numeric cards.
    static ACE = 14;
    static JACK = 11;
    static QUEEN = 12;
    static KING = 13;

    /**
     * Private fields to store suit and value.
     * In JavaScript, # denotes a private field.
     */
    #suit;
    #value;

    /**
     * Creates a card with a specified suit and value.
     * If no arguments are provided, it creates a Joker with value 1.
     * 
     * @param {number} theValue The value of the card (2-14 for regular, any for Joker).
     * @param {number} theSuit The suit of the card (SPADES, HEARTS, DIAMONDS, CLUBS, or JOKER).
     * @throws {Error} If the parameter values are not in permissible ranges.
     */
    constructor(theValue = 1, theSuit = PokerCard.JOKER) {
        if (
            theSuit !== PokerCard.SPADES &&
            theSuit !== PokerCard.HEARTS &&
            theSuit !== PokerCard.DIAMONDS &&
            theSuit !== PokerCard.CLUBS &&
            theSuit !== PokerCard.JOKER
        ) {
            throw new Error("Illegal playing card suit");
        }

        if (theSuit !== PokerCard.JOKER && (theValue < 2 || theValue > 14)) {
            throw new Error("Illegal playing card value");
        }

        this.#value = theValue;
        this.#suit = theSuit;

    }

    /**
     * Returns the suit of this card.
     * @returns {number}
     */
    getSuit() {
        return this.#suit;
    }

    /**
     * Returns the value of this card.
     * @returns {number}
     */
    getValue() {
        return this.#value;
    }

    /**
     * Returns a String representation of the card's suit.
     * @returns {string}
     */
    getSuitAsString() {
        switch (this.#suit) {
            case PokerCard.SPADES:   return "Spades";
            case PokerCard.HEARTS:   return "Hearts";
            case PokerCard.DIAMONDS: return "Diamonds";
            case PokerCard.CLUBS:    return "Clubs";
            default:                 return "Joker";
        }
    }

    /**
     * Returns a String representation of the card's value.
     * @returns {string}
     */
    getValueAsString() {
        if (this.#suit === PokerCard.JOKER) {
            return "" + this.#value;
        } else {
            switch (this.#value) {
                case 2:  return "2";
                case 3:  return "3";
                case 4:  return "4";
                case 5:  return "5";
                case 6:  return "6";
                case 7:  return "7";
                case 8:  return "8";
                case 9:  return "9";
                case 10: return "10";
                case 11: return "Jack";
                case 12: return "Queen";
                case 13: return "King";
                default: return "Ace";
            }
        }
    }

    /**
     * Returns a string representation of this card.
     * @returns {string}
     */
    toString() {
        if (this.#suit === PokerCard.JOKER) {
            if (this.#value === 1) {
                return "Joker";
            } else {
                return `Joker #${this.#value}`;
            }
        } else {
            return `${this.getValueAsString()} of ${this.getSuitAsString()}`;
        }
    }

    /**
     * Checks whether this card equals another card.
     * @param {Object} obj The object to compare with.
     * @returns {boolean}
     */
    equals(obj) {
        if (obj === null || !(obj instanceof PokerCard)) {
            return false;
        }
        return (this.#suit === obj.getSuit() && this.#value === obj.getValue());
    }
}

/**
 * PokerRank.js
 *
 * Java -> JavaScript translation of the provided PokerRank Java class.
 *
 * Assumes existence of a PokerCard class with:
 *  - instance methods: getValue() -> Number, getSuit() -> Number
 *  - static constants: JOKER, ACE
 *
 *  This is a utility class that can be used to assign ranks
 *  to poker hands containing up to five cards.  It does not
 *  handle jokers or hands of more than five cards, but a rank
 *  can be computed for a hand with fewer than five cards.
 *  A numerical rank is assigned to a hand.  One poker hand
 *  beats another if and only if the rank for the first hand
 *  is greater than the rank for the second hand.  If the
 *  ranks are equal, then two hands are tied.  Note that a hand
 *  with fewer than five cards is never considered to be
 *  a straight or flush.
 *  Once cards have been added to a PokerRank object, the
 *  numerical rank can be obtained by calling the getRank()
 *  method.  Call getDescription() to get a verbal description 
 *  of the hand that is good enough for most purposes (such as
 *  "Pair of Kings"), but that does not include enough information
 *  to fully rank the hand. Call getLongDescription() to get a
 *  verbal description with enough detail to fully rank the hand.
 */

class PokerRank {

  // Constants for basic types of poker hands.
  static NOTHING = 0;
  static PAIR = 1;
  static TWO_PAIR = 2;
  static TRIPLE = 3;
  static STRAIGHT = 4;
  static FLUSH = 5;
  static FULL_HOUSE = 6;
  static FOUR_OF_A_KIND = 7;
  static STRAIGHT_FLUSH = 8;
  static ROYAL_FLUSH = 9;

  /**
   * Construct a PokerRank object.
   * Accepts either a list of PokerCard arguments, or a single array of PokerCard.
   * Example:
   *   new PokerRank(card1, card2)
   *   new PokerRank([card1, card2])
   *
   * Throws Error on invalid cards or too many cards.
   */
  constructor(...cards) {
    // Internal array of cards (will be manipulated during computeRank)
    this.cards = [];

    // Numerical rank; -1 indicates "not computed yet"
    this.rank = -1;

    // Description strings
    this.description = '';
    this.longDescription = '';

    // If single argument and it's an array, treat as list of cards
    if (cards.length === 1 && Array.isArray(cards[0])) {
      cards = cards[0];
    }
    if (cards != null) {
      for (const c of cards) {
        this.add(c);
      }
    }
  }

  /**
   * Adds an array of PokerCards to this hand.
   * Throws Error if card is null, is a joker, or would make more than 5 cards.
   */
  add(card) {
    if (card == null) {
      throw new Error("Cards can't be null for class PokerRank");
    }
    if (card.getSuit && card.getSuit() === PokerCard.JOKER) {
      throw new Error("Class PokerRank does not support jokers.");
    }
    if (this.cards.length === 5) {
      throw new Error("PokerRank does not support hands with more than five cards.");
    }
    this.cards.push(card);
    this.rank = -1;
  }

  /**
   * Remove all cards so this object can be reused.
   */
  clear() {
    this.cards.length = 0;
    this.rank = -1;
  }

  /**
   * Return the numerical rank. Compute it if necessary.
   */
  getRank() {
    if (this.rank === -1) this.computeRank();
    return this.rank;
  }

  /**
   * Short description (may not fully disambiguate two equal hand types).
   */
  getDescription() {
    if (this.rank === -1) this.computeRank();
    return this.description;
  }

  /**
   * Long description (contains enough detail to fully rank).
   */
  getLongDescription() {
    if (this.rank === -1) this.computeRank();
    return this.longDescription;
  }

  /**
   * Returns the basic hand type constant (NOTHING..ROYAL_FLUSH).
   */
  getHandType() {
    if (this.rank === -1) this.computeRank();
    // use unsigned right shift to ensure correct non-negative result
    return this.rank >>> 20;
  }

  /**
   * Human-readable hand type string.
   */
  getHandTypeAsString() {
    if (this.cards.length === 0) return "Empty Hand";
    const type = this.getHandType();
    if (type === PokerRank.PAIR) return "Pair";
    if (type === PokerRank.TWO_PAIR) return "Two pairs";
    if (type === PokerRank.TRIPLE) return "Triple";
    if (type === PokerRank.STRAIGHT) return "Straight";
    if (type === PokerRank.FLUSH) return "Flush";
    if (type === PokerRank.FULL_HOUSE) return "Full House";
    if (type === PokerRank.FOUR_OF_A_KIND) return "Four of a kind";
    if (type === PokerRank.STRAIGHT_FLUSH) return "Straight Flush";
    if (type === PokerRank.ROYAL_FLUSH) return "Royal Flush";
    return "Nothing";
  }

  /**
   * Returns a shallow copy of the cards in the order used for ranking.
   * Ensures the rank has been computed (so cards might be rearranged accordingly).
   */
  getCards() {
    if (this.rank === -1) this.computeRank();
    return this.cards.slice();
  }

  /**
   * toString returns same as getDescription()
   */
  toString() {
    return this.getDescription();
  }

  // ----------------- helper methods, not meant for use outside this class ---------------------

  valueName(c) {
    switch (c.getValue()) {
      case 2: return "Two";
      case 3: return "Three";
      case 4: return "Four";
      case 5: return "Five";
      case 6: return "Six";
      case 7: return "Seven";
      case 8: return "Eight";
      case 9: return "Nine";
      case 10: return "Ten";
      case 11: return "Jack";
      case 12: return "Queen";
      case 13: return "King";
      default: return "Ace";
    }
  }

  pluralValueName(c) {
    // Special case for six -> "Sixes" (to match original Java behavior)
    if (c.getValue() === 6) return "Sixes";
    return this.valueName(c) + "s";
  }

  cardValueNames() {
    if (this.cards.length === 0) return "";
    const names = this.cards.map(c => this.valueName(c));
    return names.join(',');
  }

  /**
   * Compute rank, description, and longDescription for the current cards.
   * This mutates this.cards to the ordering used for tie-breaking.
   */
  computeRank() {
    // If no cards
    if (this.cards.length === 0) {
      this.rank = 0;
      this.description = this.longDescription = "Empty Hand";
      return;
    }

    // Sort cards by value descending, then by suit descending (suit order only for neatness)
    // Make a shallow copy to avoid accidental external changes; then we'll assign to this.cards.
    let sorted = this.cards.slice().sort((a, b) => {
      if (a.getValue() !== b.getValue()) return b.getValue() - a.getValue();
      return b.getSuit() - a.getSuit();
    });

    this.cards = sorted;

    try {
      // Check flush (only possible for 5-card hands)
      let isFlush = false;
      if (this.cards.length === 5) {
        const s0 = this.cards[0].getSuit();
        isFlush = this.cards.every(c => c.getSuit() === s0);
      }

      // Check straight (only for 5-card hands)
      let isStraight = false;
      if (this.cards.length === 5) {
        // Handle 5-4-3-2-A (ace low straight) which after sorting is Ace,5,4,3,2
        if (this.cards[0].getValue() === PokerCard.ACE &&
            this.cards[1].getValue() === 5 &&
            this.cards[2].getValue() === 4 &&
            this.cards[3].getValue() === 3 &&
            this.cards[4].getValue() === 2) {
          isStraight = true;
          // Move the Ace from start to end
          this.cards.push(this.cards.shift());
        } else {
          // Normal straight: consecutive descending by 1
          const v0 = this.cards[0].getValue();
          isStraight = (this.cards[1].getValue() === v0 - 1) &&
                       (this.cards[2].getValue() === v0 - 2) &&
                       (this.cards[3].getValue() === v0 - 3) &&
                       (this.cards[4].getValue() === v0 - 4);
        }
      }

      if (isFlush) {
        if (isStraight) {
          if (this.cards[0].getValue() === PokerCard.ACE) {
            this.rank = PokerRank.ROYAL_FLUSH;
            this.description = this.longDescription = "Royal Flush";
          } else {
            this.rank = PokerRank.STRAIGHT_FLUSH;
            this.description = this.longDescription = this.valueName(this.cards[0]) + "-high Straight Flush";
          }
        } else {
          this.rank = PokerRank.FLUSH;
          this.description = "Flush";
          this.longDescription = "Flush (" + this.cardValueNames() + ")";
        }
        return;
      }

      if (isStraight) {
        this.rank = PokerRank.STRAIGHT;
        this.description = this.longDescription = this.valueName(this.cards[0]) + "-high Straight";
        return;
      }

      // Check for four-of-a-kind
      if (this.cards.length >= 4) {
        if (this.cards[0].getValue() === this.cards[1].getValue() &&
            this.cards[1].getValue() === this.cards[2].getValue() &&
            this.cards[2].getValue() === this.cards[3].getValue()) {
          this.rank = PokerRank.FOUR_OF_A_KIND;
          this.description = this.longDescription = "Four " + this.pluralValueName(this.cards[0]);
          if (this.cards.length === 5) {
            this.longDescription = this.description + " (plus " + this.valueName(this.cards[4]) + ")";
          }
          return;
        }
      }
      if (this.cards.length === 5 &&
          this.cards[1].getValue() === this.cards[2].getValue() &&
          this.cards[2].getValue() === this.cards[3].getValue() &&
          this.cards[3].getValue() === this.cards[4].getValue()) {
        // Quad is at positions 1-4; move first card (the kicker) to the end.
        this.cards.push(this.cards.shift());
        this.rank = PokerRank.FOUR_OF_A_KIND;
        this.description = "Four " + this.pluralValueName(this.cards[0]);
        this.longDescription = this.description + " (plus " + this.valueName(this.cards[4]) + ")";
        return;
      }

      // Check for triples and pairs
      let tripleValue = 0;
      let tripleLocation = -1;
      for (let i = 0; i <= this.cards.length - 3; i++) {
        if (this.cards[i].getValue() === this.cards[i+1].getValue() &&
            this.cards[i+1].getValue() === this.cards[i+2].getValue()) {
          tripleLocation = i;
          tripleValue = this.cards[i].getValue();
          break;
        }
      }

      let pairValue1 = 0, pairLoc1 = -1;
      let pairValue2 = 0, pairLoc2 = -1;
      for (let i = 0; i <= this.cards.length - 2; i++) {
        if (this.cards[i].getValue() === this.cards[i+1].getValue() && this.cards[i].getValue() !== tripleValue) {
          pairValue1 = this.cards[i].getValue();
          pairLoc1 = i;
          for (let j = i+2; j <= this.cards.length - 2; j++) {
            if (this.cards[j].getValue() === this.cards[j+1].getValue() && this.cards[j].getValue() !== tripleValue) {
              pairValue2 = this.cards[j].getValue();
              pairLoc2 = j;
              break;
            }
          }
          break;
        }
      }

      if (tripleValue === 0 && pairValue1 === 0) {
        // No triple or pair -> High card
        this.rank = PokerRank.NOTHING;
        this.description = "High Card (" + this.valueName(this.cards[0]) + ")";
        this.longDescription = "High Card (" + this.cardValueNames() + ")";
        return;
      }

      if (tripleValue > 0) {
        // Move preceding cards before triple to the end (rotate)
        for (let i = 0; i < tripleLocation; i++) {
          this.cards.push(this.cards.shift());
        }
        if (pairValue1 > 0) {
          // Full house (triple then pair)
          this.rank = PokerRank.FULL_HOUSE;
          this.description = this.longDescription = "Full House, " + this.pluralValueName(this.cards[0]) +
            " and " + this.pluralValueName(this.cards[4]);
          return;
        } else {
          // Only triple
          this.rank = PokerRank.TRIPLE;
          this.description = this.longDescription = "Three " + this.pluralValueName(this.cards[0]);
          if (this.cards.length === 4) {
            this.longDescription = this.description + " (plus " + this.valueName(this.cards[3]) + ")";
          } else if (this.cards.length === 5) {
            this.longDescription = this.description + " (plus " + this.valueName(this.cards[3]) +
              " and " + this.valueName(this.cards[4]) + ")";
          }
          return;
        }
      }

      // If first pair is not at start, move it to start
      if (pairLoc1 > 0) {
        // remove pair from their positions and add to front in same order
        const p2 = this.cards.splice(pairLoc1+1, 1)[0];
        const p1 = this.cards.splice(pairLoc1, 1)[0];
        this.cards.unshift(p2);
        this.cards.unshift(p1);
      }

      if (pairValue2 === 0) {
        // Only one pair
        this.rank = PokerRank.PAIR;
        this.description = this.longDescription = "Pair of " + this.pluralValueName(this.cards[0]);
        if (this.cards.length === 5) {
          this.longDescription = this.description + " (plus " + this.valueName(this.cards[2]) + "," +
            this.valueName(this.cards[3]) + "," + this.valueName(this.cards[4]) + ")";
        } else if (this.cards.length === 4) {
          this.longDescription = this.description + " (plus " + this.valueName(this.cards[2]) + "," +
            this.valueName(this.cards[3]) + ")";
        } else if (this.cards.length === 3) {
          this.longDescription = this.description + " (plus " + this.valueName(this.cards[2]) + ")";
        }
        return;
      }

      // Two pairs
      if (pairLoc2 > 2) {
        // Move second pair so that it starts at position 2
        const p2 = this.cards.splice(pairLoc2+1, 1)[0];
        const p1 = this.cards.splice(pairLoc2, 1)[0];
        this.cards.splice(2, 0, p2);
        this.cards.splice(2, 0, p1);
      }

      this.rank = PokerRank.TWO_PAIR;
      this.description = this.longDescription = "Two Pairs, " + this.pluralValueName(this.cards[0]) + " and " +
        this.pluralValueName(this.cards[2]);
      if (this.cards.length === 5) {
        this.longDescription = this.description + " (plus " + this.valueName(this.cards[4]) + ")";
      }

    } finally {
      // In the finally block, add the card values into the lower 20 bits and move the hand type into bits 20-23.
      // The previous assignments to this.rank have been the hand type only (0..9).
      // Shift up into bits 20-23:
      this.rank <<= 20;
      // Add card values into bits 19-0: first card -> bits 19-16, second -> 15-12, ...
      // If fewer than 5 cards, lower bits remain zero.
      for (let i = 0; i < this.cards.length; i++) {
        // Compute shift for this card: 4*(4-i)
        const shift = 4 * (4 - i);
        // Use bitwise OR to insert the value
        this.rank |= (this.cards[i].getValue() << shift);
      }
      // Ensure rank is a 32-bit integer (JS bitwise ops already do that, but make explicit)
      this.rank = this.rank | 0;
    }
  }
}

