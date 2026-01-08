function Card(value, suit) {
    var theSuit, theValue;
    this.getSuit = function() { return theSuit; };
    this.getValue = function() { return theValue; };
    if (arguments.length == 0) {
        theSuit = Card.JOKER;
        theValue = 1;
    }
    else if (arguments.length == 1) {
        throw "No suit specified for card.";
    }
    else {
        if (typeof value == "string" && isNaN(Number(value))) {
            switch (value.toUpperCase()) {
                case "ACE": theValue = Card.ACE; break;
                case "KING": theValue = Card.KING; break;
                case "QUEEN": theValue = Card.QUEEN; break;
                case "JACK": theValue = Card.JACK; break;
                default: throw "Illegal value string for card: '" + value + "'";
            }
        }
        else {
            var v = Number(value);
            if (isNaN(v) || v != Math.round(v) || v < 1 || v > 13) {
                throw "Illegal value for card: '" + value + "'";
            }
            theValue = v;
        }
        if (typeof suit == "string") {
            switch (suit.toUpperCase()) {
                case "HEART": case "HEARTS": theSuit = Card.HEARTS; break;
                case "SPADE": case "SPADES": theSuit = Card.SPADES; break;
                case "DIAMOND": case "DIAMONDS": theSuit = Card.DIAMONDS; break;
                case "CLUB": case "CLUBS": theSuit = Card.CLUBS; break;
                case "JOKER": theSuit = Card.JOKER; break;
                default: throw "Illegal suit string for card: '" + suit + "'";
            }
        }
        else {
            var s = Number(suit);
            if (isNaN(s) || s != Math.round(s) || s < 0 || s > 3) {
                throw "Illegal suit number for card: '" + suit + "'";
            }
            theSuit = s;
        }
    }
}
Card.imageFolder = "cards-90x126";
Card.ACE = 1;
Card.JACK = 11;
Card.QUEEN = 12;
Card.KING = 13;
Card.SPADES = 0;
Card.HEARTS = 1;
Card.CLUBS = 2;
Card.DIAMONDS = 3;
Card.JOKER = 4;
Card.prototype.toString = function() {
    if (this.getSuit() == Card.JOKER) {
       if (this.getValue() == 1)
          return "Joker";
       else
          return "Joker #" + this.getValue();
    }
    else
       return this.getValueAsString() + " of " + this.getSuitAsString();
}
Card.prototype.getImageURL = function() {
    return Card.imageFolder + "/" + this.getImageFileName();
}
Card.prototype.getFaceDownImageURL = function() {
    return Card.imageFolder + "/" + "back.jpg";
}
Card.prototype.getImageFileName = function() {
    if (this.getSuit() == Card.JOKER) {
        return "joker.jpg";
    }
    var v,s;
    if (this.getValue() == 1 || this.getValue() > 10) {
        v = this.getValueAsString().charAt(0);
    }
    else {
        v = "" + this.getValue();
    }
    s = this.getSuitAsString().toLowerCase().charAt(0);
    return s + v + ".jpg";
}
Card.prototype.getValueAsString = function() {
    if (this.getSuit() == Card.JOKER)
       return "" + this.getValue();
    else {
       switch ( this.getValue() ) {
            case 1:   return "Ace";
            case 2:   return "2";
            case 3:   return "3";
            case 4:   return "4";
            case 5:   return "5";
            case 6:   return "6";
            case 7:   return "7";
            case 8:   return "8";
            case 9:   return "9";
            case 10:  return "10";
            case 11:  return "Jack";
            case 12:  return "Queen";
            default:  return "King";
       }
    }
}
Card.prototype.getSuitAsString = function() {
    switch ( this.getSuit() ) {
        case Card.SPADES:   return "Spades";
        case Card.HEARTS:   return "Hearts";
        case Card.DIAMONDS: return "Diamonds";
        case Card.CLUBS:    return "Clubs";
        default:       return "Joker";
    }
}


function Deck() {
    var count;
    var cards = new Array(52);
    this.cardsLeft = function() {
        return count;
    };
    this.nextCard = function() {
        if (count == 0) {
            throw "Can't deal a card because deck is empty.";
        }
        count = count - 1;
        return cards[count];
    }
    this.shuffle = function() { 
        count = 52;
        for (var i = 52; i > 1; i--) {
            var r = Math.floor(Math.random() * i);
            var t = cards[r];
            cards[r] = cards[i-1];
            cards[i-1] = t;
        }
    }
    var k = 0;
    for (var s = 0; s < 4; s++) {
        for (var v = 1; v <= 13; v++) {
            cards[k] = new Card(v,s);
            k++;
        }
    }
    this.shuffle();
}


/**
 *  Find the type of poker hand, given a hand consisting of 5 cards.  The return
 *  value is appropriate for checking the value of a hand in Video Poker.  The
 *  return value is always one of the following strings:
 *
 *     "Royal flush"
 *     "Straight flush"
 *     "Full house"
 *     "Four of a kind"
 *     "Flush"
 *     "Straight"
 *     "Three of a kind"
 *     "Two pairs"
 *     "High pair"  (meaning one pair, Jacks or better)
 *     "Low pair"  (meaning one pair, lower than Jacks)
 *     "No hand"
 *
 *  This function can take either 5 parameters, where each parameter is a Card,
 *  or an array of length 5 in which each element is a Card.  The cards must all
 *  be distinct, since the function can't handle the case of duplicate cards.
 */
function computeRank() {

      var cards, i, j, k;
      
      if (arguments.length == 0) {
          throw "No cards were passed as parameters";
      }
      if (arguments.length > 1) {
          cards = arguments; 
      }
      else {
          cards = arguments[0];
      }
      if (! cards.length || cards.length != 5) {
          throw "Five cards are required as parameters.";
      }
      for (i = 0; i < 5; i++) {
          if ( ! (cards[i] instanceof Card) ) {
              throw "Parameter values must be Cards";
          }
          if (i > 0 && cards[i].getValue() == cards[i-1].getValue() && cards[i].getSuit() == cards[i-1].getSuit()) {
              throw "Parameter values cannot include duplicate cards.";
          }
      }
      
      /* Sort the cards by value.  Within the same value, sort them by
       * suit just to be neat; the suit order has no effect on the rank. */
      
      var newCards = new Array(5);  // work with copy of the array.
      for (i = 0; i < 5; i++) {
          newCards[i] = cards[i];
      }
      cards = newCards;
      for (k = 4; k > 0; k--)  {
         var maxCard = 0;
         for (i = 1; i <= k; i++) {
            if (cards[i].getValue() > cards[maxCard].getValue() ||
                  cards[i].getValue() == cards[maxCard].getValue() && cards[i].getSuit() > cards[maxCard].getSuit())
               maxCard = i;
         }
         var temp = cards[k];
         cards[k] = cards[maxCard];
         cards[maxCard] = temp;
      }
      
      /* Check if the card is a straight and/or flush.
       */

      var isFlush =  cards[0].getSuit() == cards[1].getSuit() 
                     && cards[1].getSuit() == cards[2].getSuit()
                     && cards[1].getSuit() == cards[3].getSuit() 
                     && cards[1].getSuit() == cards[4].getSuit();

      var isStraight = (cards[1].getValue() == cards[0].getValue() + 1
                     && cards[2].getValue() == cards[1].getValue() + 1
                     && cards[3].getValue() == cards[2].getValue() + 1
                     && cards[4].getValue() == cards[3].getValue() + 1) ||
                       (cards[1].getValue() == 10
                     && cards[2].getValue() == Card.JACK
                     && cards[3].getValue() == Card.QUEEN
                     && cards[4].getValue() == Card.KING
                     && cards[0].getValue() == Card.ACE);
      
      if (isFlush) {
         if (isStraight) {
            if (cards[4].getValue() == Card.KING && cards[0].getValue() == Card.ACE) {
               return "Royal flush";
            }
            else {
               return "Straight flush";
            }
         }
         else { 
            return "Flush";
         }
      }

      if (isStraight)  {
         return "Straight";
      }

      /* Check for four-of-a-kind, first one in which the four-of-a-kind
       * occurs in the first four cards, then in the case where the first
       * card is not part of the four-of-a-kind. 
       */

      if (cards[0].getValue() == cards[1].getValue()
                  && cards[1].getValue() == cards[2].getValue()
                  && cards[2].getValue() == cards[3].getValue()) {
         return "Four of a kind";
      }
      if (cards[1].getValue() == cards[2].getValue()
            && cards[2].getValue() == cards[3].getValue()
            && cards[3].getValue() == cards[4].getValue()) {
         return "Four of a kind";
      }

      /* Check for triples and pairs. */

      var tripleValue = 0;     // If greater than 0, then there is a triple with this value.
      for (i = 0; i <= 2; i++) {
         if (cards[i].getValue() == cards[i+1].getValue()
               && cards[i+1].getValue() == cards[i+2].getValue()) {
            tripleValue = cards[i].getValue();
            break;
         }
      }
      var pairValue1 = 0; // If greater than 0, then there is a pair with this value.  If two pairs, this is the first value.
      var pairValue2 = 0; // If greater than 0, there are two pairs and this is the value of the cards in the second pair.
      for (i = 0; i <= 3; i++) {
               // Look for a pair at position i.  Be careful not to count two cards that
               // are part of a triple as being a pair
         if (cards[i].getValue() == cards[i+1].getValue() && cards[i].getValue() != tripleValue) {
                // Found a pair at position i.  Record it and look for a second pair later in the hand.
            pairValue1 = cards[i].getValue();
            for (j = i+2; j <= 3; j++) {
               if (cards[j].getValue() == cards[j+1].getValue() && cards[j].getValue() != tripleValue) {
                    // Found a second pair.
                  pairValue2 = cards[j].getValue();
                  break;
               }
            }
            break;
         }
      }

      if (tripleValue == 0 && pairValue1 == 0) {
            // No triple or pair in the hand.
         return "No hand";
      }

      if (tripleValue > 0) { 
             // There is a triple.
         if (pairValue1 > 0) {
            return "Full house";
         }
         else {
            return "Three of a kind";
         }
      }

      if (pairValue2 == 0) {  //  There was only one pair.
          if (pairValue1 < Card.JACK && pairValue1 != Card.ACE) {
              return "Low pair";
          }
          else {
              return "High pair";
          }
      }

      // If we reach this point, there are two pairs.

      return "Two pairs";
      
}
