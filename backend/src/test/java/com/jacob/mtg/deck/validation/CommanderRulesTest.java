package com.jacob.mtg.deck.validation;

import static com.jacob.mtg.deck.validation.TestCards.basicMountain;
import static com.jacob.mtg.deck.validation.TestCards.codes;
import static com.jacob.mtg.deck.validation.TestCards.commander;
import static com.jacob.mtg.deck.validation.TestCards.edhCard;
import static com.jacob.mtg.deck.validation.TestCards.filler;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.jacob.mtg.deck.Board;

class CommanderRulesTest {

    private static final List<String> RED = List.of("R");
    private static final List<String> RED_WHITE = List.of("R", "W");

    private final CommanderRules rules = new CommanderRules();

    /** commander + 99 = 100 */
    private static List<ValidationCard> legalDeck() {
        List<ValidationCard> cards = new ArrayList<>();
        cards.add(commander("Krenko, Mob Boss", RED));
        cards.addAll(filler(69, RED));
        cards.add(basicMountain(30));
        return cards;
    }

    @Test
    void hundredCards_withCommander_isLegal() {
        ValidationResult result = rules.validate(legalDeck());
        assertThat(result.issues()).isEmpty();
        assertThat(result.commanderCount()).isEqualTo(1);
    }

    @Test
    void ninetyNineCards_isWrongSize() {
        List<ValidationCard> cards = legalDeck();
        cards.remove(cards.size() - 2); // drop one filler
        assertThat(codes(rules.validate(cards))).containsExactly("WRONG_DECK_SIZE");
    }

    @Test
    void noCommander() {
        List<ValidationCard> cards = new ArrayList<>(filler(70, RED));
        cards.add(basicMountain(30));
        assertThat(codes(rules.validate(cards))).containsExactly("NO_COMMANDER");
    }

    @Test
    void singleton_secondCopyIsAnError_butBasicsAreFine() {
        List<ValidationCard> cards = legalDeck();
        cards.remove(1);
        cards.add(edhCard("Filler 1", RED)); // now "Filler 1" twice
        assertThat(codes(rules.validate(cards))).containsExactly("TOO_MANY_COPIES");
    }

    @Test
    void cardOutsideColorIdentity() {
        List<ValidationCard> cards = legalDeck();
        cards.remove(1);
        cards.add(edhCard("Counterspell", List.of("U")));
        ValidationResult result = rules.validate(cards);
        assertThat(codes(result)).containsExactly("COLOR_IDENTITY");
        assertThat(result.issues().get(0).cardName()).isEqualTo("Counterspell");
    }

    @Test
    void nonLegendaryCommander_isInvalid() {
        List<ValidationCard> cards = legalDeck();
        cards.set(0, new ValidationCard("Goblin Guide", "Creature — Goblin Scout", "", RED, List.of(), "legal", Board.COMMANDER, 1));
        assertThat(codes(rules.validate(cards))).containsExactly("INVALID_COMMANDER");
    }

    @Test
    void planeswalkerThatCanBeYourCommander_isValid() {
        List<ValidationCard> cards = legalDeck();
        cards.set(0, new ValidationCard("Commander Walker", "Legendary Planeswalker — Test",
            "Commander Walker can be your commander.", RED, List.of(), "legal", Board.COMMANDER, 1));
        assertThat(rules.validate(cards).legal()).isTrue();
    }

    @Test
    void twoPartners_combineColorIdentity() {
        List<ValidationCard> cards = new ArrayList<>();
        cards.add(commander("Partner Red", RED, "Partner"));
        cards.add(commander("Partner White", List.of("W"), "Partner"));
        cards.add(edhCard("White Card", List.of("W")));
        cards.add(edhCard("Boros Card", RED_WHITE));
        cards.addAll(filler(66, RED));
        cards.add(basicMountain(30));
        assertThat(rules.validate(cards).issues()).isEmpty();
    }

    @Test
    void twoCommanders_withoutPartner_isInvalid() {
        List<ValidationCard> cards = new ArrayList<>();
        cards.add(commander("Legend A", RED));
        cards.add(commander("Legend B", RED));
        cards.addAll(filler(68, RED));
        cards.add(basicMountain(30));
        assertThat(codes(rules.validate(cards))).containsExactly("INVALID_PARTNERS");
    }

    @Test
    void chooseABackground_pairsWithABackground() {
        List<ValidationCard> cards = new ArrayList<>();
        cards.add(commander("Background Chooser", RED, "Choose a Background"));
        cards.add(new ValidationCard("Some Background", "Legendary Enchantment — Background", "", List.of("W"),
            List.of(), "legal", Board.COMMANDER, 1));
        cards.addAll(filler(68, RED));
        cards.add(basicMountain(30));
        assertThat(rules.validate(cards).issues()).isEmpty();
    }

    @Test
    void partnerWith_mustNameTheOtherCommander() {
        ValidationCard pir = new ValidationCard("Pir, Imaginative Rascal", "Legendary Creature — Human",
            "Partner with Toothy, Imaginary Friend", List.of("G"), List.of("Partner with"), "legal", Board.COMMANDER, 1);
        ValidationCard toothy = new ValidationCard("Toothy, Imaginary Friend", "Legendary Creature — Illusion",
            "Partner with Pir, Imaginative Rascal", List.of("U"), List.of("Partner with"), "legal", Board.COMMANDER, 1);
        ValidationCard stranger = commander("Some Stranger", List.of("U"));

        assertThat(CommanderRules.isValidPair(pir, toothy)).isTrue();
        assertThat(CommanderRules.isValidPair(pir, stranger) || CommanderRules.isValidPair(stranger, pir)).isFalse();
    }

    @Test
    void bannedCard() {
        List<ValidationCard> cards = legalDeck();
        cards.remove(1);
        cards.add(new ValidationCard("Banned Thing", "Artifact", "", List.of(), List.of(), "banned", Board.MAIN, 1));
        assertThat(codes(rules.validate(cards))).containsExactly("BANNED");
    }

    @Test
    void relentlessRats_anyNumberEvenInSingleton() {
        List<ValidationCard> cards = new ArrayList<>();
        cards.add(commander("Black Legend", List.of("B")));
        cards.add(new ValidationCard("Relentless Rats", "Creature — Rat",
            "A deck can have any number of cards named Relentless Rats.", List.of("B"), List.of(), "legal", Board.MAIN, 99));
        assertThat(rules.validate(cards).issues()).isEmpty();
    }
}
