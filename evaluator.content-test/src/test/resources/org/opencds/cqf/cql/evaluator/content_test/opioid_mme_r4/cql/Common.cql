library Common version '3.0.0'

/*
Model-independent common functionality for CQL
Note that these functions are only required to support simulating an `expand per day` in CQL 1.3
*/

define Months: { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 }

define Days28: { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
  11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
  21, 22, 23, 24, 25, 26, 27, 28
}
define Days29: { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
  11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
  21, 22, 23, 24, 25, 26, 27, 28, 29
}
define Days30: { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
  11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
  21, 22, 23, 24, 25, 26, 27, 28, 29, 30
}
define Days31: { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
  11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
  21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
  31
}

/*
Returns true if the given year is a leap year, false otherwise
*/
define function IsLeapYear(y Integer):
  (y mod 4 = 0 and y mod 100 != 0) or y mod 400 = 0


/*
Returns a list of the days in the given month in the given year
List<DateTime>
*/
define function DaysInMonth(y Integer, m Integer):
  (
    case
      when m in { 1, 3, 5, 7, 8, 10, 12 } then Days31
      when m = 2 then if IsLeapYear(y) then Days29 else Days28
      else Days30
    end
  ) D
    return DateTime(y, m, D)

/*
Returns a list of the days in the given year
List<DateTime>
*/
define function DaysInYear(y Integer):
  flatten (
    Months M
      return DaysInMonth(y, M)
  )

define function DaysInPeriod(period Interval<DateTime>):
  case
    when start of period is null or end of period is null then null
    when start of period same year as end of period then
      case
        when month from start of period = month from end of period then
          (DaysInMonth(year from start of period, month from start of period)) D
            where D same day or after start of period
              and D same day or before end of period
        else
          (DaysInYear(year from start of period)) D
            where D same day or after start of period
              and D same day or before end of period
      end
    when difference in years between start of period and end of period = 1 then
      (
        (DaysInYear(year from start of period)) D
          where D same day or after start of period
            and D same day or before end of period
        )
        union (
          (DaysInYear(year from end of period)) D
            where D same day or after start of period
              and D same day or before end of period
          )
    else null // Should maybe throw, can't calculate arbitrary years, need the expand operator (1.3)
  end
