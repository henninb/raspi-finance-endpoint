#!/usr/bin/env python3
"""
ServiceMac Mortgage Principal Calculator

Based on analysis of servicemac_brian account payments.
ServiceMac credits extra payments at the START of the month for interest calculation.

Usage:
    python mortgage-calculator.py                     # Interactive mode
    python mortgage-calculator.py 95347.03 6042      # Single month
    python mortgage-calculator.py 95347.03 6000 --months 12  # Project 12 months
"""

import argparse
import sys

# Mortgage constants
ANNUAL_RATE = 0.0649  # 6.49%
MONTHLY_RATE = ANNUAL_RATE / 12
MONTHLY_PAYMENT = 1294.39


def calculate_month(balance: float, extra_payment: float) -> dict:
    """
    Calculate a single month's mortgage payment breakdown.

    Args:
        balance: Current principal balance at start of month
        extra_payment: Total extra principal payment for the month

    Returns:
        dict with interest, principal, and new balance
    """
    # ServiceMac credits extra payment at start of month for interest calc
    balance_for_interest = balance - extra_payment
    interest = balance_for_interest * MONTHLY_RATE
    principal = MONTHLY_PAYMENT - interest
    new_balance = balance - extra_payment - principal

    return {
        "starting_balance": balance,
        "extra_payment": extra_payment,
        "balance_for_interest": balance_for_interest,
        "interest": round(interest, 2),
        "principal": round(principal, 2),
        "total_paid": round(extra_payment + MONTHLY_PAYMENT, 2),
        "new_balance": round(new_balance, 2)
    }


def project_months(starting_balance: float, monthly_extra: float, num_months: int) -> list:
    """
    Project multiple months of payments.

    Args:
        starting_balance: Current principal balance
        monthly_extra: Extra payment to make each month
        num_months: Number of months to project

    Returns:
        List of monthly calculation results
    """
    results = []
    balance = starting_balance

    for month in range(1, num_months + 1):
        # Adjust extra payment if it would exceed remaining balance
        extra = min(monthly_extra, balance - MONTHLY_PAYMENT * 0.1)
        if extra < 0:
            extra = 0

        result = calculate_month(balance, extra)
        result["month"] = month
        results.append(result)

        balance = result["new_balance"]
        if balance <= 0:
            break

    return results


def print_single_month(result: dict):
    """Print a single month's calculation."""
    print("\nMortgage Payment Calculation")
    print("=" * 50)
    print(f"Starting balance:        ${result['starting_balance']:>12,.2f}")
    print(f"Extra payment:           ${result['extra_payment']:>12,.2f}")
    print(f"Balance for interest:    ${result['balance_for_interest']:>12,.2f}")
    print(f"Monthly rate:            {MONTHLY_RATE * 100:>12.6f}%")
    print("-" * 50)
    print(f"Interest portion:        ${result['interest']:>12,.2f}")
    print(f"Principal portion:       ${result['principal']:>12,.2f}")
    print(f"Regular payment:         ${MONTHLY_PAYMENT:>12,.2f}")
    print(f"Extra payment:           ${result['extra_payment']:>12,.2f}")
    print(f"Total paid:              ${result['total_paid']:>12,.2f}")
    print("-" * 50)
    print(f"New balance:             ${result['new_balance']:>12,.2f}")


def print_projection(results: list):
    """Print a multi-month projection table."""
    print("\nMortgage Payment Projection")
    print("=" * 95)
    print(f"{'Month':>5} | {'Balance':>12} | {'Extra':>10} | {'Interest':>10} | {'Principal':>10} | {'New Balance':>12}")
    print("-" * 95)

    total_interest = 0
    total_principal = 0
    total_extra = 0

    for r in results:
        print(f"{r['month']:>5} | ${r['starting_balance']:>10,.2f} | ${r['extra_payment']:>8,.2f} | "
              f"${r['interest']:>8,.2f} | ${r['principal']:>8,.2f} | ${r['new_balance']:>10,.2f}")
        total_interest += r['interest']
        total_principal += r['principal']
        total_extra += r['extra_payment']

    print("-" * 95)
    print(f"{'TOTAL':>5} | {'':>12} | ${total_extra:>8,.2f} | ${total_interest:>8,.2f} | ${total_principal:>8,.2f} |")
    print()
    print(f"Total payments: ${total_extra + len(results) * MONTHLY_PAYMENT:,.2f}")
    print(f"Principal paid: ${total_principal + total_extra:,.2f}")
    print(f"Interest paid:  ${total_interest:,.2f}")

    if results[-1]['new_balance'] <= 0:
        print(f"\nMortgage paid off in {len(results)} months!")


def interactive_mode():
    """Run in interactive mode."""
    print("\nServiceMac Mortgage Calculator")
    print("=" * 50)
    print(f"Annual Rate: {ANNUAL_RATE * 100:.2f}%")
    print(f"Monthly Payment: ${MONTHLY_PAYMENT:,.2f}")
    print()

    try:
        balance = float(input("Enter current balance: $"))
        extra = float(input("Enter extra payment for this month: $"))

        result = calculate_month(balance, extra)
        print_single_month(result)

        project = input("\nProject future months? (y/n): ").strip().lower()
        if project == 'y':
            months = int(input("How many months to project? "))
            monthly_extra = float(input("Monthly extra payment amount: $"))
            results = project_months(balance, monthly_extra, months)
            print_projection(results)

    except ValueError as e:
        print(f"Invalid input: {e}")
        sys.exit(1)
    except KeyboardInterrupt:
        print("\nCancelled.")
        sys.exit(0)


def main():
    parser = argparse.ArgumentParser(
        description="Calculate mortgage principal portions using ServiceMac's method",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s                          Interactive mode
  %(prog)s 95347.03 6042            Calculate single month
  %(prog)s 95347.03 6000 -m 12      Project 12 months with $6000/month extra
  %(prog)s 95347.03 6000 -m 24 -v   Verbose projection
        """
    )
    parser.add_argument("balance", type=float, nargs="?", help="Current principal balance")
    parser.add_argument("extra", type=float, nargs="?", help="Extra payment this month")
    parser.add_argument("-m", "--months", type=int, default=1, help="Number of months to project")
    parser.add_argument("-v", "--verbose", action="store_true", help="Show detailed output")

    args = parser.parse_args()

    if args.balance is None:
        interactive_mode()
    else:
        if args.extra is None:
            print("Error: extra payment amount required")
            sys.exit(1)

        if args.months == 1:
            result = calculate_month(args.balance, args.extra)
            if args.verbose:
                print_single_month(result)
            else:
                print(f"${result['principal']:.2f}")
        else:
            results = project_months(args.balance, args.extra, args.months)
            print_projection(results)


if __name__ == "__main__":
    main()
