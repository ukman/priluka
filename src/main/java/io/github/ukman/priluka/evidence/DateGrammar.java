package io.github.ukman.priluka.evidence;

import io.github.ukman.priluka.annotation.Keyword;
import io.github.ukman.priluka.annotation.Terminal;

/**
 * Reusable grammar for date-like evidence in procurement and contract text.
 */
public final class DateGrammar {
    public static final class DateEvidence {
        private final DateEvidenceExpression expression;

        DateEvidence(Date date) {
            this.expression = new SingleDateEvidence(date);
        }

        DateEvidence(DateRange range) {
            this.expression = range;
        }

        DateEvidence(MonthRange range) {
            this.expression = range;
        }

        DateEvidence(DayMonthRange range) {
            this.expression = range;
        }

        DateEvidence(YearRange range) {
            this.expression = range;
        }

        public String kind() {
            return expression.kind();
        }
    }

    public interface DateEvidenceExpression {
        String kind();
    }

    static final class SingleDateEvidence implements DateEvidenceExpression {
        private final Date date;

        SingleDateEvidence(Date date) {
            this.date = date;
        }

        @Override
        public String kind() {
            return date.kind();
        }
    }

    public static final class Date {
        private final DateExpression expression;

        Date(DateExpression expression) {
            this.expression = expression;
        }

        public String kind() {
            return expression.kind();
        }
    }

    public interface DateExpression {
        String kind();
    }

    public static final class DateRange implements DateEvidenceExpression {
        DateRange(Date from, RangeConnector connector, Date to) {
        }

        DateRange(From fromWord, Date from, To toWord, Date to) {
        }

        DateRange(Between between, Date from, And and, Date to) {
        }

        @Override
        public String kind() {
            return "date-range";
        }
    }

    public interface RangeConnector {}
    public interface MonthRangeConnector {}

    static final class ToConnector implements RangeConnector {
        ToConnector(To to) {
        }
    }

    static final class MonthToConnector implements MonthRangeConnector {
        MonthToConnector(To to) {
        }
    }

    static final class UntilConnector implements RangeConnector {
        UntilConnector(Until until) {
        }
    }

    static final class MonthUntilConnector implements MonthRangeConnector {
        MonthUntilConnector(Until until) {
        }
    }

    static final class ThroughConnector implements RangeConnector {
        ThroughConnector(Through through) {
        }
    }

    static final class MonthThroughConnector implements MonthRangeConnector {
        MonthThroughConnector(Through through) {
        }
    }

    static final class DashConnector implements RangeConnector {
        DashConnector(Dash dash) {
        }
    }

    static final class MonthDashConnector implements MonthRangeConnector {
        MonthDashConnector(Dash dash) {
        }
    }

    static final class EnDashConnector implements RangeConnector {
        EnDashConnector(EnDash dash) {
        }
    }

    static final class MonthEnDashConnector implements MonthRangeConnector {
        MonthEnDashConnector(EnDash dash) {
        }
    }

    public static final class MonthRange implements DateEvidenceExpression {
        MonthRange(TextMonth from, MonthRangeConnector connector, TextMonth to) {
        }

        MonthRange(Between between, TextMonth from, And and, TextMonth to) {
        }

        MonthRange(From fromWord, TextMonth from, To toWord, TextMonth to) {
        }

        @Override
        public String kind() {
            return "month-range";
        }
    }

    public static final class DayMonthRange implements DateEvidenceExpression {
        DayMonthRange(DayMonth from, RangeConnector connector, DayMonth to) {
        }

        DayMonthRange(DayMonth from, RangeConnector connector, DayMonth to, Year year) {
        }

        DayMonthRange(Between between, DayMonth from, And and, DayMonth to) {
        }

        DayMonthRange(Between between, DayMonth from, And and, DayMonth to, Year year) {
        }

        DayMonthRange(Between between, SharedDay from, And and, SharedDay to, TextMonth month) {
        }

        DayMonthRange(Between between, SharedDay from, And and, SharedDay to, TextMonth month, Year year) {
        }

        DayMonthRange(Weekday fromWeekday, DayMonth from, To toWord, Weekday toWeekday, DayMonth to, Inc inc) {
        }

        @Override
        public String kind() {
            return "day-month-range";
        }
    }

    public static final class YearRange implements DateEvidenceExpression {
        YearRange(FullYear from, RangeConnector connector, FullYear to) {
        }

        YearRange(Between between, FullYear from, And and, FullYear to) {
        }

        YearRange(From fromWord, FullYear from, To toWord, FullYear to) {
        }

        @Override
        public String kind() {
            return "year-range";
        }
    }

    public static final class DeadlineEvidence {
        DeadlineEvidence(DeadlineSignal signal, DeadlineDate date) {
        }

        DeadlineEvidence(DeadlineSignal signal, DeadlineSeparator separator, DeadlineDate date) {
        }

        DeadlineEvidence(DeadlineSignal signal, TimeLead time, DeadlineDate date) {
        }

        DeadlineEvidence(DeadlineSignal signal, DeadlineSeparator separator, TimeLead time, DeadlineDate date) {
        }

        public String kind() {
            return "deadline-evidence";
        }
    }

    public interface DeadlineSignal {}
    public interface DeadlineSeparator {}

    static final class SubmissionDeadline implements DeadlineSignal {
        SubmissionDeadline(Submission submission, Deadline deadline) {
        }
    }

    static final class TenderSubmissionDeadline implements DeadlineSignal {
        TenderSubmissionDeadline(Tender tender, Submission submission, Deadline deadline) {
        }
    }

    static final class BidSubmissionDeadline implements DeadlineSignal {
        BidSubmissionDeadline(Bid bid, Submission submission, Deadline deadline) {
        }
    }

    static final class IttSubmissionDeadline implements DeadlineSignal {
        IttSubmissionDeadline(Itt itt, Submission submission, Deadline deadline) {
        }
    }

    static final class ResponseSubmissionDeadline implements DeadlineSignal {
        ResponseSubmissionDeadline(Response response, Submission submission, Deadline deadline) {
        }
    }

    static final class PsqResponseSubmissionDeadline implements DeadlineSignal {
        PsqResponseSubmissionDeadline(Psq psq, Response response, Submission submission, Deadline deadline) {
        }
    }

    static final class ClarificationDeadline implements DeadlineSignal {
        ClarificationDeadline(Clarification clarification, Deadline deadline) {
        }
    }

    static final class ClarificationsDeadline implements DeadlineSignal {
        ClarificationsDeadline(Clarifications clarifications, Deadline deadline) {
        }
    }

    static final class InitialClarificationDeadline implements DeadlineSignal {
        InitialClarificationDeadline(Initial initial, Clarification clarification, Deadline deadline) {
        }
    }

    static final class AuthorityDeadlineForResponsesToClarifications implements DeadlineSignal {
        AuthorityDeadlineForResponsesToClarifications(
            Authority authority,
            Deadline deadline,
            For forWord,
            Responses responses,
            To to,
            Clarifications clarifications
        ) {
        }
    }

    static final class AuthorityDeadlineToRespondToClarifications implements DeadlineSignal {
        AuthorityDeadlineToRespondToClarifications(
            Authority authority,
            Deadline deadline,
            To to,
            Respond respond,
            To toSecond,
            Clarifications clarifications
        ) {
        }
    }

    static final class DeadlineForReceiptOfClarifications implements DeadlineSignal {
        DeadlineForReceiptOfClarifications(Deadline deadline, For forWord, Receipt receipt, Of of, Clarifications clarifications) {
        }
    }

    static final class ClarificationResponsesIssuedBy implements DeadlineSignal {
        ClarificationResponsesIssuedBy(Clarification clarification, Responses responses, To to, Be be, Issued issued, By by) {
        }
    }

    static final class InitialClarificationPeriodOpens implements DeadlineSignal {
        InitialClarificationPeriodOpens(Initial initial, Clarification clarification, Period period, Opens opens) {
        }
    }

    static final class InitialClarificationPeriodCloses implements DeadlineSignal {
        InitialClarificationPeriodCloses(Initial initial, Clarification clarification, Period period, Closes closes) {
        }
    }

    static final class TenderClarificationPeriodCloses implements DeadlineSignal {
        TenderClarificationPeriodCloses(Tender tender, Clarification clarification, Period period, Closes closes) {
        }
    }

    static final class ClosingDate implements DeadlineSignal {
        ClosingDate(Closing closing, DateWord date) {
        }
    }

    static final class ClosingDateForReturnOfSubmission implements DeadlineSignal {
        ClosingDateForReturnOfSubmission(
            Closing closing,
            DateWord date,
            For forWord,
            Return returnWord,
            Of of,
            Submission submission
        ) {
        }
    }

    static final class ContractStartDate implements DeadlineSignal {
        ContractStartDate(Contract contract, Start start, DateWord date) {
        }
    }

    static final class ContractCompletionDate implements DeadlineSignal {
        ContractCompletionDate(Contract contract, Completion completion, DateWord date) {
        }
    }

    static final class ContractCommencementDate implements DeadlineSignal {
        ContractCommencementDate(Contract contract, Commencement commencement, DateWord date) {
        }
    }

    static final class ContractCommencementDateAndBeginningOfMobilisationPeriod implements DeadlineSignal {
        ContractCommencementDateAndBeginningOfMobilisationPeriod(
            Contract contract,
            Commencement commencement,
            DateWord date,
            And and,
            Beginning beginning,
            Of of,
            Mobilisation mobilisation,
            Period period
        ) {
        }
    }

    static final class ContractAwardNotice implements DeadlineSignal {
        ContractAwardNotice(Contract contract, Award award, Notice notice) {
        }
    }

    static final class IssueOfContractAwardNotice implements DeadlineSignal {
        IssueOfContractAwardNotice(Issue issue, Of of, Contract contract, Award award, Notice notice) {
        }
    }

    static final class NoLaterThanSignal implements DeadlineSignal {
        NoLaterThanSignal(No no, Later later, Than than) {
        }
    }

    static final class CompleteBySignal implements DeadlineSignal {
        CompleteBySignal(Complete complete, By by) {
        }
    }

    static final class DeadlineDate {
        DeadlineDate(Date date) {
        }

        DeadlineDate(Weekday weekday, Date date) {
        }
    }

    static final class TimeLead {
        TimeLead(ClockTime time, On on) {
        }

        TimeLead(ClockTime time, Noon noon, On on) {
        }

        TimeLead(ClockTime time, Hrs hrs, On on) {
        }

        TimeLead(ClockTime time, OpenParen open, Noon noon, CloseParen close, On on) {
        }

        TimeLead(ClockTime time, Hrs hrs, OpenParen open, Noon noon, CloseParen close, On on) {
        }
    }

    static final class ClockTime {
        ClockTime(Day hour, Colon colon, Minute minute) {
        }
    }

    public interface Minute {}

    static final class ColonSeparator implements DeadlineSeparator {
        ColonSeparator(Colon colon) {
        }
    }

    static final class PipeSeparator implements DeadlineSeparator {
        PipeSeparator(Pipe pipe) {
        }
    }

    static final class DeadlineDashSeparator implements DeadlineSeparator {
        DeadlineDashSeparator(Dash dash) {
        }
    }

    static final class DeadlineEnDashSeparator implements DeadlineSeparator {
        DeadlineEnDashSeparator(EnDash dash) {
        }
    }

    static final class DayMonth {
        DayMonth(Day day, TextMonth month) {
        }

        DayMonth(Day day, OrdinalSuffix suffix, TextMonth month) {
        }

        DayMonth(Day day, Of of, TextMonth month) {
        }

        DayMonth(Day day, OrdinalSuffix suffix, Of of, TextMonth month) {
        }

        DayMonth(The the, Day day, TextMonth month) {
        }

        DayMonth(The the, Day day, OrdinalSuffix suffix, TextMonth month) {
        }

        DayMonth(The the, Day day, Of of, TextMonth month) {
        }

        DayMonth(The the, Day day, OrdinalSuffix suffix, Of of, TextMonth month) {
        }
    }

    static final class SharedDay {
        SharedDay(Day day) {
        }

        SharedDay(Day day, OrdinalSuffix suffix) {
        }
    }

    public interface Weekday {}

    static final class DayMonthYear implements DateExpression {
        DayMonthYear(Day day, TextMonth month, Year year) {
        }

        DayMonthYear(Day day, OrdinalSuffix suffix, TextMonth month, Year year) {
        }

        @Override
        public String kind() {
            return "day-month-year";
        }
    }

    static final class SlashDate implements DateExpression {
        SlashDate(Day day, Slash slash1, Month month, Slash slash2, Year year) {
        }

        @Override
        public String kind() {
            return "slash-date";
        }
    }

    static final class IsoSlashDate implements DateExpression {
        IsoSlashDate(FullYear year, Slash slash1, Month month, Slash slash2, Day day) {
        }

        @Override
        public String kind() {
            return "iso-slash-date";
        }
    }

    static final class DotDate implements DateExpression {
        DotDate(Day day, Dot dot1, Month month, Dot dot2, Year year) {
        }

        @Override
        public String kind() {
            return "dot-date";
        }
    }

    static final class DashDate implements DateExpression {
        DashDate(Day day, Dash dash1, Month month, Dash dash2, Year year) {
        }

        @Override
        public String kind() {
            return "dash-date";
        }
    }

    static final class IsoDashDate implements DateExpression {
        IsoDashDate(FullYear year, Dash dash1, Month month, Dash dash2, Day day) {
        }

        @Override
        public String kind() {
            return "iso-dash-date";
        }
    }

    static final class MonthDayYear implements DateExpression {
        MonthDayYear(TextMonth month, Day day, Year year) {
        }

        MonthDayYear(TextMonth month, Day day, Comma comma, Year year) {
        }

        MonthDayYear(TextMonth month, Dot dot, Day day, Year year) {
        }

        MonthDayYear(TextMonth month, Dot dot, Day day, Comma comma, Year year) {
        }

        MonthDayYear(TextMonth month, Day day, OrdinalSuffix suffix, Year year) {
        }

        MonthDayYear(TextMonth month, Day day, OrdinalSuffix suffix, Comma comma, Year year) {
        }

        MonthDayYear(TextMonth month, Dot dot, Day day, OrdinalSuffix suffix, Year year) {
        }

        MonthDayYear(TextMonth month, Dot dot, Day day, OrdinalSuffix suffix, Comma comma, Year year) {
        }

        @Override
        public String kind() {
            return "month-day-year";
        }
    }

    static final class MonthYear implements DateExpression {
        MonthYear(NumericMonth month, FullYear year) {
        }

        MonthYear(NumericMonth month, Dot dot, FullYear year) {
        }

        MonthYear(TextMonth month, Year year) {
        }

        MonthYear(TextMonth month, Dot dot, Year year) {
        }

        @Override
        public String kind() {
            return "month-year";
        }
    }

    public interface Year {}
    public interface FullYear extends Year {}
    public interface ShortYear extends Year {}
    public interface Month {}
    public interface NumericMonth extends Month {}
    public interface TextMonth extends Month {}
    public interface Day {}
    public interface OrdinalSuffix {}

    public static final class Years {
        @Keyword(value = "1990", priority = 300) public static final class Year1990 implements FullYear {}
        @Keyword(value = "1991", priority = 300) public static final class Year1991 implements FullYear {}
        @Keyword(value = "1992", priority = 300) public static final class Year1992 implements FullYear {}
        @Keyword(value = "1993", priority = 300) public static final class Year1993 implements FullYear {}
        @Keyword(value = "1994", priority = 300) public static final class Year1994 implements FullYear {}
        @Keyword(value = "1995", priority = 300) public static final class Year1995 implements FullYear {}
        @Keyword(value = "1996", priority = 300) public static final class Year1996 implements FullYear {}
        @Keyword(value = "1997", priority = 300) public static final class Year1997 implements FullYear {}
        @Keyword(value = "1998", priority = 300) public static final class Year1998 implements FullYear {}
        @Keyword(value = "1999", priority = 300) public static final class Year1999 implements FullYear {}
        @Keyword(value = "2000", priority = 300) public static final class Year2000 implements FullYear {}
        @Keyword(value = "2001", priority = 300) public static final class Year2001 implements FullYear {}
        @Keyword(value = "2002", priority = 300) public static final class Year2002 implements FullYear {}
        @Keyword(value = "2003", priority = 300) public static final class Year2003 implements FullYear {}
        @Keyword(value = "2004", priority = 300) public static final class Year2004 implements FullYear {}
        @Keyword(value = "2005", priority = 300) public static final class Year2005 implements FullYear {}
        @Keyword(value = "2006", priority = 300) public static final class Year2006 implements FullYear {}
        @Keyword(value = "2007", priority = 300) public static final class Year2007 implements FullYear {}
        @Keyword(value = "2008", priority = 300) public static final class Year2008 implements FullYear {}
        @Keyword(value = "2009", priority = 300) public static final class Year2009 implements FullYear {}
        @Keyword(value = "2010", priority = 300) public static final class Year2010 implements FullYear {}
        @Keyword(value = "2011", priority = 300) public static final class Year2011 implements FullYear {}
        @Keyword(value = "2012", priority = 300) public static final class Year2012 implements FullYear {}
        @Keyword(value = "2013", priority = 300) public static final class Year2013 implements FullYear {}
        @Keyword(value = "2014", priority = 300) public static final class Year2014 implements FullYear {}
        @Keyword(value = "2015", priority = 300) public static final class Year2015 implements FullYear {}
        @Keyword(value = "2016", priority = 300) public static final class Year2016 implements FullYear {}
        @Keyword(value = "2017", priority = 300) public static final class Year2017 implements FullYear {}
        @Keyword(value = "2018", priority = 300) public static final class Year2018 implements FullYear {}
        @Keyword(value = "2019", priority = 300) public static final class Year2019 implements FullYear {}
        @Keyword(value = "2020", priority = 300) public static final class Year2020 implements FullYear {}
        @Keyword(value = "2021", priority = 300) public static final class Year2021 implements FullYear {}
        @Keyword(value = "2022", priority = 300) public static final class Year2022 implements FullYear {}
        @Keyword(value = "2023", priority = 300) public static final class Year2023 implements FullYear {}
        @Keyword(value = "2024", priority = 300) public static final class Year2024 implements FullYear {}
        @Keyword(value = "2025", priority = 300) public static final class Year2025 implements FullYear {}
        @Keyword(value = "2026", priority = 300) public static final class Year2026 implements FullYear {}
        @Keyword(value = "2027", priority = 300) public static final class Year2027 implements FullYear {}
        @Keyword(value = "2028", priority = 300) public static final class Year2028 implements FullYear {}
        @Keyword(value = "2029", priority = 300) public static final class Year2029 implements FullYear {}
        @Keyword(value = "2030", priority = 300) public static final class Year2030 implements FullYear {}
        @Keyword(value = "2031", priority = 300) public static final class Year2031 implements FullYear {}
        @Keyword(value = "2032", priority = 300) public static final class Year2032 implements FullYear {}
        @Keyword(value = "2033", priority = 300) public static final class Year2033 implements FullYear {}
        @Keyword(value = "2034", priority = 300) public static final class Year2034 implements FullYear {}
        @Keyword(value = "2035", priority = 300) public static final class Year2035 implements FullYear {}
        @Keyword(value = "21", priority = 300) public static final class YearShort21 implements ShortYear {}
        @Keyword(value = "22", priority = 300) public static final class YearShort22 implements ShortYear {}
        @Keyword(value = "23", priority = 300) public static final class YearShort23 implements ShortYear {}
        @Keyword(value = "24", priority = 300) public static final class YearShort24 implements ShortYear {}
        @Keyword(value = "25", priority = 300) public static final class YearShort25 implements ShortYear {}
        @Keyword(value = "26", priority = 300) public static final class YearShort26 implements ShortYear {}
        @Keyword(value = "27", priority = 300) public static final class YearShort27 implements ShortYear {}
        @Keyword(value = "28", priority = 300) public static final class YearShort28 implements ShortYear {}
        @Keyword(value = "29", priority = 300) public static final class YearShort29 implements ShortYear {}
        @Keyword(value = "30", priority = 300) public static final class YearShort30 implements ShortYear {}
    }

    public static final class Days {
        @Keyword(value = "1", priority = 250) public static final class Day1 implements Day {}
        @Keyword(value = "01", priority = 250) public static final class DayZero1 implements Day {}
        @Keyword(value = "2", priority = 250) public static final class Day2 implements Day {}
        @Keyword(value = "02", priority = 250) public static final class DayZero2 implements Day {}
        @Keyword(value = "3", priority = 250) public static final class Day3 implements Day {}
        @Keyword(value = "03", priority = 250) public static final class DayZero3 implements Day {}
        @Keyword(value = "4", priority = 250) public static final class Day4 implements Day {}
        @Keyword(value = "04", priority = 250) public static final class DayZero4 implements Day {}
        @Keyword(value = "5", priority = 250) public static final class Day5 implements Day {}
        @Keyword(value = "05", priority = 250) public static final class DayZero5 implements Day {}
        @Keyword(value = "6", priority = 250) public static final class Day6 implements Day {}
        @Keyword(value = "06", priority = 250) public static final class DayZero6 implements Day {}
        @Keyword(value = "7", priority = 250) public static final class Day7 implements Day {}
        @Keyword(value = "07", priority = 250) public static final class DayZero7 implements Day {}
        @Keyword(value = "8", priority = 250) public static final class Day8 implements Day {}
        @Keyword(value = "08", priority = 250) public static final class DayZero8 implements Day {}
        @Keyword(value = "9", priority = 250) public static final class Day9 implements Day {}
        @Keyword(value = "09", priority = 250) public static final class DayZero9 implements Day {}
        @Keyword(value = "10", priority = 250) public static final class Day10 implements Day {}
        @Keyword(value = "11", priority = 250) public static final class Day11 implements Day {}
        @Keyword(value = "12", priority = 250) public static final class Day12 implements Day {}
        @Keyword(value = "13", priority = 250) public static final class Day13 implements Day {}
        @Keyword(value = "14", priority = 250) public static final class Day14 implements Day {}
        @Keyword(value = "15", priority = 250) public static final class Day15 implements Day {}
        @Keyword(value = "16", priority = 250) public static final class Day16 implements Day {}
        @Keyword(value = "17", priority = 250) public static final class Day17 implements Day {}
        @Keyword(value = "18", priority = 250) public static final class Day18 implements Day {}
        @Keyword(value = "19", priority = 250) public static final class Day19 implements Day {}
        @Keyword(value = "20", priority = 250) public static final class Day20 implements Day {}
        @Keyword(value = "21", priority = 250) public static final class Day21 implements Day {}
        @Keyword(value = "22", priority = 250) public static final class Day22 implements Day {}
        @Keyword(value = "23", priority = 250) public static final class Day23 implements Day {}
        @Keyword(value = "24", priority = 250) public static final class Day24 implements Day {}
        @Keyword(value = "25", priority = 250) public static final class Day25 implements Day {}
        @Keyword(value = "26", priority = 250) public static final class Day26 implements Day {}
        @Keyword(value = "27", priority = 250) public static final class Day27 implements Day {}
        @Keyword(value = "28", priority = 250) public static final class Day28 implements Day {}
        @Keyword(value = "29", priority = 250) public static final class Day29 implements Day {}
        @Keyword(value = "30", priority = 250) public static final class Day30 implements Day {}
        @Keyword(value = "31", priority = 250) public static final class Day31 implements Day {}
    }

    public static final class Months {
        @Keyword(value = "1", priority = 240) public static final class Month1 implements NumericMonth {}
        @Keyword(value = "01", priority = 240) public static final class MonthZero1 implements NumericMonth {}
        @Keyword(value = "2", priority = 240) public static final class Month2 implements NumericMonth {}
        @Keyword(value = "02", priority = 240) public static final class MonthZero2 implements NumericMonth {}
        @Keyword(value = "3", priority = 240) public static final class Month3 implements NumericMonth {}
        @Keyword(value = "03", priority = 240) public static final class MonthZero3 implements NumericMonth {}
        @Keyword(value = "4", priority = 240) public static final class Month4 implements NumericMonth {}
        @Keyword(value = "04", priority = 240) public static final class MonthZero4 implements NumericMonth {}
        @Keyword(value = "5", priority = 240) public static final class Month5 implements NumericMonth {}
        @Keyword(value = "05", priority = 240) public static final class MonthZero5 implements NumericMonth {}
        @Keyword(value = "6", priority = 240) public static final class Month6 implements NumericMonth {}
        @Keyword(value = "06", priority = 240) public static final class MonthZero6 implements NumericMonth {}
        @Keyword(value = "7", priority = 240) public static final class Month7 implements NumericMonth {}
        @Keyword(value = "07", priority = 240) public static final class MonthZero7 implements NumericMonth {}
        @Keyword(value = "8", priority = 240) public static final class Month8 implements NumericMonth {}
        @Keyword(value = "08", priority = 240) public static final class MonthZero8 implements NumericMonth {}
        @Keyword(value = "9", priority = 240) public static final class Month9 implements NumericMonth {}
        @Keyword(value = "09", priority = 240) public static final class MonthZero9 implements NumericMonth {}
        @Keyword(value = "10", priority = 240) public static final class Month10 implements NumericMonth {}
        @Keyword(value = "11", priority = 240) public static final class Month11 implements NumericMonth {}
        @Keyword(value = "12", priority = 240) public static final class Month12 implements NumericMonth {}
        @Keyword(value = "January", caseSensitive = false, priority = 260) public static final class MonthJanuary implements TextMonth {}
        @Keyword(value = "Jan", caseSensitive = false, priority = 260) public static final class MonthJan implements TextMonth {}
        @Keyword(value = "February", caseSensitive = false, priority = 260) public static final class MonthFebruary implements TextMonth {}
        @Keyword(value = "Feb", caseSensitive = false, priority = 260) public static final class MonthFeb implements TextMonth {}
        @Keyword(value = "March", caseSensitive = false, priority = 260) public static final class MonthMarch implements TextMonth {}
        @Keyword(value = "Mar", caseSensitive = false, priority = 260) public static final class MonthMar implements TextMonth {}
        @Keyword(value = "April", caseSensitive = false, priority = 260) public static final class MonthApril implements TextMonth {}
        @Keyword(value = "Apr", caseSensitive = false, priority = 260) public static final class MonthApr implements TextMonth {}
        @Keyword(value = "May", caseSensitive = false, priority = 260) public static final class MonthMay implements TextMonth {}
        @Keyword(value = "June", caseSensitive = false, priority = 260) public static final class MonthJune implements TextMonth {}
        @Keyword(value = "Jun", caseSensitive = false, priority = 260) public static final class MonthJun implements TextMonth {}
        @Keyword(value = "July", caseSensitive = false, priority = 260) public static final class MonthJuly implements TextMonth {}
        @Keyword(value = "Jul", caseSensitive = false, priority = 260) public static final class MonthJul implements TextMonth {}
        @Keyword(value = "August", caseSensitive = false, priority = 260) public static final class MonthAugust implements TextMonth {}
        @Keyword(value = "Aug", caseSensitive = false, priority = 260) public static final class MonthAug implements TextMonth {}
        @Keyword(value = "September", caseSensitive = false, priority = 260) public static final class MonthSeptember implements TextMonth {}
        @Keyword(value = "Sep", caseSensitive = false, priority = 260) public static final class MonthSep implements TextMonth {}
        @Keyword(value = "Sept", caseSensitive = false, priority = 260) public static final class MonthSept implements TextMonth {}
        @Keyword(value = "October", caseSensitive = false, priority = 260) public static final class MonthOctober implements TextMonth {}
        @Keyword(value = "Oct", caseSensitive = false, priority = 260) public static final class MonthOct implements TextMonth {}
        @Keyword(value = "November", caseSensitive = false, priority = 260) public static final class MonthNovember implements TextMonth {}
        @Keyword(value = "Nov", caseSensitive = false, priority = 260) public static final class MonthNov implements TextMonth {}
        @Keyword(value = "December", caseSensitive = false, priority = 260) public static final class MonthDecember implements TextMonth {}
        @Keyword(value = "Dec", caseSensitive = false, priority = 260) public static final class MonthDec implements TextMonth {}
    }

    public static final class OrdinalSuffixes {
        @Keyword(value = "st", caseSensitive = false, priority = 220) public static final class St implements OrdinalSuffix {}
        @Keyword(value = "nd", caseSensitive = false, priority = 220) public static final class Nd implements OrdinalSuffix {}
        @Keyword(value = "rd", caseSensitive = false, priority = 220) public static final class Rd implements OrdinalSuffix {}
        @Keyword(value = "th", caseSensitive = false, priority = 220) public static final class Th implements OrdinalSuffix {}
    }

    public static final class Minutes {
        @Keyword(value = "00", priority = 230) public static final class Minute00 implements Minute {}
        @Keyword(value = "15", priority = 230) public static final class Minute15 implements Minute {}
        @Keyword(value = "30", priority = 230) public static final class Minute30 implements Minute {}
        @Keyword(value = "45", priority = 230) public static final class Minute45 implements Minute {}
    }

    @Keyword(value = "/", priority = 200)
    public static final class Slash {
    }

    @Keyword(value = ".", priority = 200)
    public static final class Dot {
    }

    @Keyword(value = "-", priority = 200)
    public static final class Dash {
    }

    @Keyword(value = "–", priority = 200)
    public static final class EnDash {
    }

    @Keyword(value = ",", priority = 200)
    public static final class Comma {
    }

    @Keyword(value = ":", priority = 200)
    public static final class Colon {
    }

    @Keyword(value = "|", priority = 200)
    public static final class Pipe {
    }

    @Keyword(value = "(", priority = 200)
    public static final class OpenParen {
    }

    @Keyword(value = ")", priority = 200)
    public static final class CloseParen {
    }

    @Keyword(value = "to", caseSensitive = false, priority = 210)
    public static final class To {
    }

    @Keyword(value = "from", caseSensitive = false, priority = 210)
    public static final class From {
    }

    @Keyword(value = "between", caseSensitive = false, priority = 210)
    public static final class Between {
    }

    @Keyword(value = "and", caseSensitive = false, priority = 210)
    public static final class And {
    }

    @Keyword(value = "until", caseSensitive = false, priority = 210)
    public static final class Until {
    }

    @Keyword(value = "through", caseSensitive = false, priority = 210)
    public static final class Through {
    }

    @Keyword(value = "the", caseSensitive = false, priority = 210)
    public static final class The {
    }

    @Keyword(value = "of", caseSensitive = false, priority = 210)
    public static final class Of {
    }

    @Keyword(value = "for", caseSensitive = false, priority = 210)
    public static final class For {
    }

    @Keyword(value = "inc", caseSensitive = false, priority = 210)
    public static final class Inc {
    }

    @Keyword(value = "submission", caseSensitive = false, priority = 210)
    public static final class Submission {
    }

    @Keyword(value = "deadline", caseSensitive = false, priority = 210)
    public static final class Deadline {
    }

    @Keyword(value = "tender", caseSensitive = false, priority = 210)
    public static final class Tender {
    }

    @Keyword(value = "response", caseSensitive = false, priority = 210)
    public static final class Response {
    }

    @Keyword(value = "responses", caseSensitive = false, priority = 210)
    public static final class Responses {
    }

    @Keyword(value = "psq", caseSensitive = false, priority = 210)
    public static final class Psq {
    }

    @Keyword(value = "clarification", caseSensitive = false, priority = 210)
    public static final class Clarification {
    }

    @Keyword(value = "clarifications", caseSensitive = false, priority = 210)
    public static final class Clarifications {
    }

    @Keyword(value = "initial", caseSensitive = false, priority = 210)
    public static final class Initial {
    }

    @Keyword(value = "authority", caseSensitive = false, priority = 210)
    public static final class Authority {
    }

    @Keyword(value = "closing", caseSensitive = false, priority = 210)
    public static final class Closing {
    }

    @Keyword(value = "date", caseSensitive = false, priority = 210)
    public static final class DateWord {
    }

    @Keyword(value = "return", caseSensitive = false, priority = 210)
    public static final class Return {
    }

    @Keyword(value = "contract", caseSensitive = false, priority = 210)
    public static final class Contract {
    }

    @Keyword(value = "start", caseSensitive = false, priority = 210)
    public static final class Start {
    }

    @Keyword(value = "completion", caseSensitive = false, priority = 210)
    public static final class Completion {
    }

    @Keyword(value = "commencement", caseSensitive = false, priority = 210)
    public static final class Commencement {
    }

    @Keyword(value = "award", caseSensitive = false, priority = 210)
    public static final class Award {
    }

    @Keyword(value = "notice", caseSensitive = false, priority = 210)
    public static final class Notice {
    }

    @Keyword(value = "issue", caseSensitive = false, priority = 210)
    public static final class Issue {
    }

    @Keyword(value = "no", caseSensitive = false, priority = 210)
    public static final class No {
    }

    @Keyword(value = "later", caseSensitive = false, priority = 210)
    public static final class Later {
    }

    @Keyword(value = "than", caseSensitive = false, priority = 210)
    public static final class Than {
    }

    @Keyword(value = "complete", caseSensitive = false, priority = 210)
    public static final class Complete {
    }

    @Keyword(value = "by", caseSensitive = false, priority = 210)
    public static final class By {
    }

    @Keyword(value = "bid", caseSensitive = false, priority = 210)
    public static final class Bid {
    }

    @Keyword(value = "itt", caseSensitive = false, priority = 210)
    public static final class Itt {
    }

    @Keyword(value = "respond", caseSensitive = false, priority = 210)
    public static final class Respond {
    }

    @Keyword(value = "receipt", caseSensitive = false, priority = 210)
    public static final class Receipt {
    }

    @Keyword(value = "be", caseSensitive = false, priority = 210)
    public static final class Be {
    }

    @Keyword(value = "issued", caseSensitive = false, priority = 210)
    public static final class Issued {
    }

    @Keyword(value = "period", caseSensitive = false, priority = 210)
    public static final class Period {
    }

    @Keyword(value = "opens", caseSensitive = false, priority = 210)
    public static final class Opens {
    }

    @Keyword(value = "closes", caseSensitive = false, priority = 210)
    public static final class Closes {
    }

    @Keyword(value = "beginning", caseSensitive = false, priority = 210)
    public static final class Beginning {
    }

    @Keyword(value = "mobilisation", caseSensitive = false, priority = 210)
    public static final class Mobilisation {
    }

    @Keyword(value = "on", caseSensitive = false, priority = 210)
    public static final class On {
    }

    @Keyword(value = "noon", caseSensitive = false, priority = 210)
    public static final class Noon {
    }

    @Keyword(value = "hrs", caseSensitive = false, priority = 210)
    public static final class Hrs {
    }

    @Keyword(value = "Mon", caseSensitive = false, priority = 210)
    public static final class Mon implements Weekday {
    }

    @Keyword(value = "Monday", caseSensitive = false, priority = 210)
    public static final class Monday implements Weekday {
    }

    @Keyword(value = "Tue", caseSensitive = false, priority = 210)
    public static final class Tue implements Weekday {
    }

    @Keyword(value = "Tues", caseSensitive = false, priority = 210)
    public static final class Tues implements Weekday {
    }

    @Keyword(value = "Tuesday", caseSensitive = false, priority = 210)
    public static final class Tuesday implements Weekday {
    }

    @Keyword(value = "Wed", caseSensitive = false, priority = 210)
    public static final class Wed implements Weekday {
    }

    @Keyword(value = "Wednesday", caseSensitive = false, priority = 210)
    public static final class Wednesday implements Weekday {
    }

    @Keyword(value = "Thu", caseSensitive = false, priority = 210)
    public static final class Thu implements Weekday {
    }

    @Keyword(value = "Thur", caseSensitive = false, priority = 210)
    public static final class Thur implements Weekday {
    }

    @Keyword(value = "Thurs", caseSensitive = false, priority = 210)
    public static final class Thurs implements Weekday {
    }

    @Keyword(value = "Thursday", caseSensitive = false, priority = 210)
    public static final class Thursday implements Weekday {
    }

    @Keyword(value = "Fri", caseSensitive = false, priority = 210)
    public static final class Fri implements Weekday {
    }

    @Keyword(value = "Friday", caseSensitive = false, priority = 210)
    public static final class Friday implements Weekday {
    }

    @Keyword(value = "Sat", caseSensitive = false, priority = 210)
    public static final class Sat implements Weekday {
    }

    @Keyword(value = "Saturday", caseSensitive = false, priority = 210)
    public static final class Saturday implements Weekday {
    }

    @Keyword(value = "Sun", caseSensitive = false, priority = 210)
    public static final class Sun implements Weekday {
    }

    @Keyword(value = "Sunday", caseSensitive = false, priority = 210)
    public static final class Sunday implements Weekday {
    }

    @Terminal(regexp = "[A-Za-z]+", priority = -100)
    public static final class Word {
    }

    @Terminal(regexp = "\\S", priority = -10000)
    public static final class Other {
    }
}
