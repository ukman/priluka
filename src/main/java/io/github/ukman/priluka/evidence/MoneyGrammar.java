package io.github.ukman.priluka.evidence;

import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.OneOrMore;
import io.github.ukman.priluka.annotation.Separator;
import io.github.ukman.priluka.annotation.Terminal;

/**
 * Reusable grammar for money amounts, ranges, percentages, and insurance limits.
 */
public final class MoneyGrammar {
    public static final class Money {
        private final MoneyExpression expression;

        Money(MoneyExpression expression) {
            this.expression = expression;
        }

        public String kind() {
            return expression.kind();
        }
    }

    public interface MoneyExpression {
        String kind();
    }

    static final class MoneyAmount implements MoneyExpression {
        MoneyAmount(PrefixCurrency currency, NumericValue value) {
        }

        MoneyAmount(PrefixCurrency currency, NumericValue value, Scale scale) {
        }

        MoneyAmount(ApproxPrefix approx, PrefixCurrency currency, NumericValue value) {
        }

        MoneyAmount(ApproxPrefix approx, PrefixCurrency currency, NumericValue value, Scale scale) {
        }

        MoneyAmount(NumericValue value, SuffixCurrency currency) {
        }

        MoneyAmount(NumericValue value, Scale scale, SuffixCurrency currency) {
        }

        MoneyAmount(ApproxPrefix approx, NumericValue value, SuffixCurrency currency) {
        }

        MoneyAmount(ApproxPrefix approx, NumericValue value, Scale scale, SuffixCurrency currency) {
        }

        @Override
        public String kind() {
            return "money-amount";
        }
    }

    static final class MoneyRange implements MoneyExpression {
        MoneyRange(MoneyAmount left, RangeConnector connector, MoneyAmount right) {
        }

        @Override
        public String kind() {
            return "money-range";
        }
    }

    static final class MoneyThreshold implements MoneyExpression {
        MoneyThreshold(ThresholdPrefix prefix, MoneyAmount amount) {
        }

        MoneyThreshold(MoneyAmount amount, ThresholdSuffix suffix) {
        }

        @Override
        public String kind() {
            return "money-threshold";
        }
    }

    static final class Percentage implements MoneyExpression {
        Percentage(NumericValue value, Percent percent) {
        }

        @Override
        public String kind() {
            return "percentage";
        }
    }

    static final class PercentageOrMoney implements MoneyExpression {
        PercentageOrMoney(Percentage percentage, Or or, MoneyAmount amount) {
        }

        @Override
        public String kind() {
            return "percentage-or-money";
        }
    }

    static final class InsuranceLimit implements MoneyExpression {
        InsuranceLimit(InsuranceType type, MoneyAmount amount) {
        }

        InsuranceLimit(InsuranceType type, Equals equals, MoneyAmount amount) {
        }

        InsuranceLimit(InsuranceType type, Is is, MoneyAmount amount) {
        }

        @Override
        public String kind() {
            return "insurance-limit";
        }
    }

    static final class NumericValue {
        NumericValue(WholeNumber whole) {
        }

        NumericValue(WholeNumber whole, DecimalPart decimal) {
        }
    }

    static final class WholeNumber {
        WholeNumber(NumberToken first) {
        }

        WholeNumber(@OneOrMore @Separator(Comma.class) NumberToken[] parts) {
        }
    }

    static final class DecimalPart {
        DecimalPart(Dot dot, NumberToken fractional) {
        }
    }

    public interface PrefixCurrency {}
    public interface SuffixCurrency {}
    public interface Scale {}
    public interface RangeConnector {}
    public interface ThresholdPrefix {}
    public interface ThresholdSuffix {}
    public interface ApproxPrefix {}
    public interface InsuranceType {}

    static final class ToConnector implements RangeConnector {
        ToConnector(To to) {
        }
    }

    static final class DashConnector implements RangeConnector {
        DashConnector(Dash dash) {
        }
    }

    static final class EnDashConnector implements RangeConnector {
        EnDashConnector(EnDash dash) {
        }
    }

    static final class UpToConnector implements RangeConnector {
        UpToConnector(Up up, To to) {
        }
    }

    static final class AndConnector implements RangeConnector {
        AndConnector(And and) {
        }
    }

    static final class OverPrefix implements ThresholdPrefix {
        OverPrefix(Over over) {
        }
    }

    static final class MoreThanPrefix implements ThresholdPrefix {
        MoreThanPrefix(More more, Than than) {
        }
    }

    static final class PlusSuffix implements ThresholdSuffix {
        PlusSuffix(Plus plus) {
        }
    }

    static final class PlusWordSuffix implements ThresholdSuffix {
        PlusWordSuffix(PlusWord plus) {
        }
    }

    static final class OrMoreSuffix implements ThresholdSuffix {
        OrMoreSuffix(Or or, More more) {
        }
    }

    static final class PublicLiabilityInsurance implements InsuranceType {
        PublicLiabilityInsurance(Public publicWord, Liability liability, Insurance insurance) {
        }
    }

    static final class ProfessionalIndemnityInsurance implements InsuranceType {
        ProfessionalIndemnityInsurance(Professional professional, Indemnity indemnity, Insurance insurance) {
        }
    }

    static final class ProductLiabilityInsurance implements InsuranceType {
        ProductLiabilityInsurance(Product product, Liability liability, Insurance insurance) {
        }
    }

    static final class ContractorsAllRiskInsurance implements InsuranceType {
        ContractorsAllRiskInsurance(Contractors contractors, All all, Risk risk, Insurance insurance) {
        }
    }

    static final class EmployersLiabilityInsurance implements InsuranceType {
        EmployersLiabilityInsurance(Employers employers, Liability liability, Insurance insurance) {
        }

        EmployersLiabilityInsurance(Employer employer, Possessive possessive, Liability liability, Insurance insurance) {
        }

        EmployersLiabilityInsurance(Employee employee, Liability liability, Insurance insurance) {
        }
    }

    static final class Possessive {
        Possessive(Apostrophe apostrophe) {
        }

        Possessive(Apostrophe apostrophe, S s) {
        }

        Possessive(RightApostrophe apostrophe) {
        }

        Possessive(RightApostrophe apostrophe, S s) {
        }
    }

    @Terminal(regexp = "[A-Za-z]+")
    public static final class Word {}

    @Terminal(regexp = "[0-9]+")
    public static final class NumberToken {}

    @Terminal(regexp = "[^A-Za-z0-9\\s]")
    public static final class Symbol {}

    @Keyword(value = "£") public static final class Pound implements PrefixCurrency {}
    @Keyword(value = "$") public static final class Dollar implements PrefixCurrency {}
    @Keyword(value = "€") public static final class EuroSign implements PrefixCurrency {}
    @Keyword(value = "¥") public static final class YenSign implements PrefixCurrency {}
    @Keyword(value = "₣") public static final class FrancSign implements PrefixCurrency {}
    @Keyword(value = "₺") public static final class LiraSign implements PrefixCurrency {}
    @Keyword(value = "₴") public static final class HryvniaSign implements PrefixCurrency {}
    @Keyword(value = "₪") public static final class ShekelSign implements PrefixCurrency {}
    @Keyword(value = "GBP", caseSensitive = false) public static final class Gbp
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "USD", caseSensitive = false) public static final class Usd
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "EUR", caseSensitive = false) public static final class Eur
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "CAD", caseSensitive = false) public static final class Cad
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "AUD", caseSensitive = false) public static final class Aud
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "NZD", caseSensitive = false) public static final class Nzd
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "JPY", caseSensitive = false) public static final class Jpy
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "CNY", caseSensitive = false) public static final class Cny
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "SAR", caseSensitive = false) public static final class Sar
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "CHF", caseSensitive = false) public static final class Chf
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "DKK", caseSensitive = false) public static final class Dkk
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "NOK", caseSensitive = false) public static final class Nok
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "SEK", caseSensitive = false) public static final class Sek
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "ISK", caseSensitive = false) public static final class Isk
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "PLN", caseSensitive = false) public static final class Pln
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "CZK", caseSensitive = false) public static final class Czk
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "HUF", caseSensitive = false) public static final class Huf
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "RON", caseSensitive = false) public static final class Ron
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "BGN", caseSensitive = false) public static final class Bgn
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "ALL", caseSensitive = false) public static final class AllCurrency
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "BAM", caseSensitive = false) public static final class Bam
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "MKD", caseSensitive = false) public static final class Mkd
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "RSD", caseSensitive = false) public static final class Rsd
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "UAH", caseSensitive = false) public static final class Uah
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "MDL", caseSensitive = false) public static final class Mdl
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "GEL", caseSensitive = false) public static final class Gel
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "TRY", caseSensitive = false) public static final class TryCurrency
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "HRK", caseSensitive = false) public static final class Hrk
        implements PrefixCurrency, SuffixCurrency {}
    @Keyword(value = "pound", caseSensitive = false) public static final class PoundWord implements SuffixCurrency {}
    @Keyword(value = "pounds", caseSensitive = false) public static final class Pounds implements SuffixCurrency {}
    @Keyword(value = "sterling", caseSensitive = false) public static final class Sterling implements SuffixCurrency {}
    @Keyword(value = "euro", caseSensitive = false) public static final class EuroWord implements SuffixCurrency {}
    @Keyword(value = "euros", caseSensitive = false) public static final class Euros implements SuffixCurrency {}
    @Keyword(value = "dollar", caseSensitive = false) public static final class DollarWord implements SuffixCurrency {}
    @Keyword(value = "dollars", caseSensitive = false) public static final class Dollars implements SuffixCurrency {}
    @Keyword(value = "yen", caseSensitive = false) public static final class Yen implements SuffixCurrency {}
    @Keyword(value = "yuan", caseSensitive = false) public static final class Yuan implements SuffixCurrency {}
    @Keyword(value = "riyal", caseSensitive = false) public static final class Riyal implements SuffixCurrency {}
    @Keyword(value = "riyals", caseSensitive = false) public static final class Riyals implements SuffixCurrency {}
    @Keyword(value = "dinar", caseSensitive = false) public static final class Dinar implements SuffixCurrency {}
    @Keyword(value = "dinars", caseSensitive = false) public static final class Dinars implements SuffixCurrency {}
    @Keyword(value = "franc", caseSensitive = false) public static final class Franc implements SuffixCurrency {}
    @Keyword(value = "francs", caseSensitive = false) public static final class Francs implements SuffixCurrency {}
    @Keyword(value = "krone", caseSensitive = false) public static final class Krone implements SuffixCurrency {}
    @Keyword(value = "kroner", caseSensitive = false) public static final class Kroner implements SuffixCurrency {}
    @Keyword(value = "krona", caseSensitive = false) public static final class Krona implements SuffixCurrency {}
    @Keyword(value = "kronor", caseSensitive = false) public static final class Kronor implements SuffixCurrency {}
    @Keyword(value = "kronur", caseSensitive = false) public static final class Kronur implements SuffixCurrency {}
    @Keyword(value = "koruna", caseSensitive = false) public static final class Koruna implements SuffixCurrency {}
    @Keyword(value = "zloty", caseSensitive = false) public static final class Zloty implements SuffixCurrency {}
    @Keyword(value = "forint", caseSensitive = false) public static final class Forint implements SuffixCurrency {}
    @Keyword(value = "lei", caseSensitive = false) public static final class Lei implements SuffixCurrency {}
    @Keyword(value = "lev", caseSensitive = false) public static final class Lev implements SuffixCurrency {}
    @Keyword(value = "leva", caseSensitive = false) public static final class Leva implements SuffixCurrency {}
    @Keyword(value = "lek", caseSensitive = false) public static final class Lek implements SuffixCurrency {}
    @Keyword(value = "mark", caseSensitive = false) public static final class Mark implements SuffixCurrency {}
    @Keyword(value = "denar", caseSensitive = false) public static final class Denar implements SuffixCurrency {}
    @Keyword(value = "dinara", caseSensitive = false) public static final class Dinara implements SuffixCurrency {}
    @Keyword(value = "kuna", caseSensitive = false) public static final class Kuna implements SuffixCurrency {}
    @Keyword(value = "hryvnia", caseSensitive = false) public static final class Hryvnia implements SuffixCurrency {}
    @Keyword(value = "hryvnias", caseSensitive = false) public static final class Hryvnias implements SuffixCurrency {}
    @Keyword(value = "leu", caseSensitive = false) public static final class Leu implements SuffixCurrency {}
    @Keyword(value = "lari", caseSensitive = false) public static final class Lari implements SuffixCurrency {}
    @Keyword(value = "lira", caseSensitive = false) public static final class Lira implements SuffixCurrency {}
    @Keyword(value = "liras", caseSensitive = false) public static final class Liras implements SuffixCurrency {}
    @Keyword(value = "shekel", caseSensitive = false) public static final class Shekel implements SuffixCurrency {}
    @Keyword(value = "shekels", caseSensitive = false) public static final class Shekels implements SuffixCurrency {}
    @Keyword(value = "kr", caseSensitive = false) public static final class KrCurrency implements SuffixCurrency {}
    @Keyword(value = "Ft", caseSensitive = false) public static final class FtCurrency implements SuffixCurrency {}
    @Keyword(value = "zl", caseSensitive = false) public static final class ZlCurrency implements SuffixCurrency {}
    @Keyword(value = "Kc", caseSensitive = false) public static final class KcCurrency implements SuffixCurrency {}
    @Keyword(value = "KM", caseSensitive = false) public static final class KmCurrency implements SuffixCurrency {}

    static final class CanadianDollar implements SuffixCurrency {
        CanadianDollar(Canadian canadian, DollarWord dollar) {
        }

        CanadianDollar(Canadian canadian, Dollars dollars) {
        }
    }

    static final class AustralianDollar implements SuffixCurrency {
        AustralianDollar(Australian australian, DollarWord dollar) {
        }

        AustralianDollar(Australian australian, Dollars dollars) {
        }
    }

    static final class NewZealandDollar implements SuffixCurrency {
        NewZealandDollar(NewWord newWord, Zealand zealand, DollarWord dollar) {
        }

        NewZealandDollar(NewWord newWord, Zealand zealand, Dollars dollars) {
        }
    }

    static final class SaudiRiyal implements SuffixCurrency {
        SaudiRiyal(Saudi saudi, Riyal riyal) {
        }

        SaudiRiyal(Saudi saudi, Riyals riyals) {
        }
    }

    static final class SaudiDinar implements SuffixCurrency {
        SaudiDinar(Saudi saudi, Dinar dinar) {
        }

        SaudiDinar(Saudi saudi, Dinars dinars) {
        }
    }

    static final class SwissFranc implements SuffixCurrency {
        SwissFranc(Swiss swiss, Franc franc) {
        }

        SwissFranc(Swiss swiss, Francs francs) {
        }
    }

    static final class DanishKrone implements SuffixCurrency {
        DanishKrone(Danish danish, Krone krone) {
        }

        DanishKrone(Danish danish, Kroner kroner) {
        }
    }

    static final class NorwegianKrone implements SuffixCurrency {
        NorwegianKrone(Norwegian norwegian, Krone krone) {
        }

        NorwegianKrone(Norwegian norwegian, Kroner kroner) {
        }
    }

    static final class SwedishKrona implements SuffixCurrency {
        SwedishKrona(Swedish swedish, Krona krona) {
        }

        SwedishKrona(Swedish swedish, Kronor kronor) {
        }
    }

    static final class IcelandicKrona implements SuffixCurrency {
        IcelandicKrona(Icelandic icelandic, Krona krona) {
        }

        IcelandicKrona(Icelandic icelandic, Kronur kronur) {
        }
    }

    static final class PolishZloty implements SuffixCurrency {
        PolishZloty(Polish polish, Zloty zloty) {
        }
    }

    static final class CzechKoruna implements SuffixCurrency {
        CzechKoruna(Czech czech, Koruna koruna) {
        }
    }

    static final class HungarianForint implements SuffixCurrency {
        HungarianForint(Hungarian hungarian, Forint forintWord) {
        }
    }

    static final class RomanianLeu implements SuffixCurrency {
        RomanianLeu(Romanian romanian, Leu leu) {
        }

        RomanianLeu(Romanian romanian, Lei lei) {
        }
    }

    static final class BulgarianLev implements SuffixCurrency {
        BulgarianLev(Bulgarian bulgarian, Lev lev) {
        }

        BulgarianLev(Bulgarian bulgarian, Leva leva) {
        }
    }

    static final class AlbanianLek implements SuffixCurrency {
        AlbanianLek(Albanian albanian, Lek lek) {
        }
    }

    static final class BosnianMark implements SuffixCurrency {
        BosnianMark(Bosnian bosnian, Mark mark) {
        }

        BosnianMark(Convertible convertible, Mark mark) {
        }
    }

    static final class MacedonianDenar implements SuffixCurrency {
        MacedonianDenar(Macedonian macedonian, Denar denar) {
        }
    }

    static final class SerbianDinar implements SuffixCurrency {
        SerbianDinar(Serbian serbian, Dinar dinar) {
        }

        SerbianDinar(Serbian serbian, Dinara dinara) {
        }
    }

    static final class CroatianKuna implements SuffixCurrency {
        CroatianKuna(Croatian croatian, Kuna kuna) {
        }
    }

    static final class UkrainianHryvnia implements SuffixCurrency {
        UkrainianHryvnia(Ukrainian ukrainian, Hryvnia hryvnia) {
        }

        UkrainianHryvnia(Ukrainian ukrainian, Hryvnias hryvnias) {
        }
    }

    static final class MoldovanLeu implements SuffixCurrency {
        MoldovanLeu(Moldovan moldovan, Leu leu) {
        }
    }

    static final class GeorgianLari implements SuffixCurrency {
        GeorgianLari(Georgian georgian, Lari lari) {
        }
    }

    static final class TurkishLira implements SuffixCurrency {
        TurkishLira(Turkish turkish, Lira lira) {
        }

        TurkishLira(Turkish turkish, Liras liras) {
        }
    }

    @Keyword(value = ",") public static final class Comma {}
    @Keyword(value = ".") public static final class Dot {}
    @Keyword(value = "-") public static final class Dash {}
    @Keyword(value = "–") public static final class EnDash {}
    @Keyword(value = "+") public static final class Plus {}
    @Keyword(value = "%") public static final class Percent {}
    @Keyword(value = "=") public static final class Equals {}
    @Keyword(value = "'") public static final class Apostrophe {}
    @Keyword(value = "’") public static final class RightApostrophe {}

    @Keyword(value = "c", caseSensitive = false) public static final class Circa implements ApproxPrefix {}
    @Keyword(value = "m", caseSensitive = false) public static final class MillionShort implements Scale {}
    @Keyword(value = "k", caseSensitive = false) public static final class ThousandShort implements Scale {}
    @Keyword(value = "b", caseSensitive = false) public static final class BillionShort implements Scale {}
    @Keyword(value = "bn", caseSensitive = false) public static final class BillionShortBn implements Scale {}
    @Keyword(value = "million", caseSensitive = false) public static final class Million implements Scale {}
    @Keyword(value = "billion", caseSensitive = false) public static final class Billion implements Scale {}
    @Keyword(value = "thousand", caseSensitive = false) public static final class Thousand implements Scale {}

    @Keyword(value = "to", caseSensitive = false) public static final class To {}
    @Keyword(value = "up", caseSensitive = false) public static final class Up {}
    @Keyword(value = "and", caseSensitive = false) public static final class And {}
    @Keyword(value = "or", caseSensitive = false) public static final class Or {}
    @Keyword(value = "over", caseSensitive = false) public static final class Over {}
    @Keyword(value = "more", caseSensitive = false) public static final class More {}
    @Keyword(value = "than", caseSensitive = false) public static final class Than {}
    @Keyword(value = "plus", caseSensitive = false) public static final class PlusWord {}
    @Keyword(value = "is", caseSensitive = false) public static final class Is {}
    @Keyword(value = "canadian", caseSensitive = false) public static final class Canadian {}
    @Keyword(value = "australian", caseSensitive = false) public static final class Australian {}
    @Keyword(value = "new", caseSensitive = false) public static final class NewWord {}
    @Keyword(value = "zealand", caseSensitive = false) public static final class Zealand {}
    @Keyword(value = "saudi", caseSensitive = false) public static final class Saudi {}
    @Keyword(value = "swiss", caseSensitive = false) public static final class Swiss {}
    @Keyword(value = "danish", caseSensitive = false) public static final class Danish {}
    @Keyword(value = "norwegian", caseSensitive = false) public static final class Norwegian {}
    @Keyword(value = "swedish", caseSensitive = false) public static final class Swedish {}
    @Keyword(value = "icelandic", caseSensitive = false) public static final class Icelandic {}
    @Keyword(value = "polish", caseSensitive = false) public static final class Polish {}
    @Keyword(value = "czech", caseSensitive = false) public static final class Czech {}
    @Keyword(value = "hungarian", caseSensitive = false) public static final class Hungarian {}
    @Keyword(value = "romanian", caseSensitive = false) public static final class Romanian {}
    @Keyword(value = "bulgarian", caseSensitive = false) public static final class Bulgarian {}
    @Keyword(value = "albanian", caseSensitive = false) public static final class Albanian {}
    @Keyword(value = "bosnian", caseSensitive = false) public static final class Bosnian {}
    @Keyword(value = "convertible", caseSensitive = false) public static final class Convertible {}
    @Keyword(value = "macedonian", caseSensitive = false) public static final class Macedonian {}
    @Keyword(value = "serbian", caseSensitive = false) public static final class Serbian {}
    @Keyword(value = "croatian", caseSensitive = false) public static final class Croatian {}
    @Keyword(value = "ukrainian", caseSensitive = false) public static final class Ukrainian {}
    @Keyword(value = "moldovan", caseSensitive = false) public static final class Moldovan {}
    @Keyword(value = "georgian", caseSensitive = false) public static final class Georgian {}
    @Keyword(value = "turkish", caseSensitive = false) public static final class Turkish {}

    @Keyword(value = "public", caseSensitive = false) public static final class Public {}
    @Keyword(value = "professional", caseSensitive = false) public static final class Professional {}
    @Keyword(value = "product", caseSensitive = false) public static final class Product {}
    @Keyword(value = "contractors", caseSensitive = false) public static final class Contractors {}
    @Keyword(value = "employers", caseSensitive = false) public static final class Employers {}
    @Keyword(value = "employer", caseSensitive = false) public static final class Employer {}
    @Keyword(value = "employee", caseSensitive = false) public static final class Employee {}
    @Keyword(value = "liability", caseSensitive = false) public static final class Liability {}
    @Keyword(value = "indemnity", caseSensitive = false) public static final class Indemnity {}
    @Keyword(value = "insurance", caseSensitive = false) public static final class Insurance {}
    @Keyword(value = "all", caseSensitive = false) public static final class All {}
    @Keyword(value = "risk", caseSensitive = false) public static final class Risk {}
    @Keyword(value = "s", caseSensitive = false) public static final class S {}
}
