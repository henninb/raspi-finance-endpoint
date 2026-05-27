#!/usr/bin/env python3
"""
Mortgage Principal Calculator

Supports lenders that credit extra payments at the START of the month
for interest calculation (e.g., ServiceMac).

Usage:
    python mortgage-calculator.py 17538              # Extra payment only (balance from saved state)
    python mortgage-calculator.py 46534.45 17538     # Balance + extra payment
    python mortgage-calculator.py 46534.45 6000 -m 12  # Project 12 months
"""

import argparse
import json
import os
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path

from rich.console import Console
from rich.panel import Panel
from rich.table import Table
from rich import box

MAX_REASONABLE_BALANCE = 10_000_000.0
MAX_MONTHS = 360

DEFAULT_ANNUAL_RATE = 0.0649
DEFAULT_MONTHLY_PAYMENT = 1294.39

STATE_FILE = Path(
    os.environ.get(
        "MORTGAGE_STATE_FILE",
        str(Path.home() / ".mortgage-calculator.json"),
    )
)

console = Console()


@dataclass(frozen=True)
class MortgageConfig:
    annual_rate: float
    monthly_payment: float

    @property
    def monthly_rate(self) -> float:
        return self.annual_rate / 12

    def validate(self):
        if not (0.0 < self.annual_rate < 1.0):
            raise ValueError(f"Annual rate must be between 0 and 1, got {self.annual_rate}")
        if self.monthly_payment <= 0:
            raise ValueError(f"Monthly payment must be positive, got {self.monthly_payment}")


def _validate_amount(value: float, name: str) -> float:
    if value < 0:
        raise ValueError(f"{name} cannot be negative, got {value}")
    if value > MAX_REASONABLE_BALANCE:
        raise ValueError(
            f"{name} exceeds maximum allowed ({MAX_REASONABLE_BALANCE:,.0f}), got {value:,.2f}"
        )
    return value


def load_state() -> dict:
    if not STATE_FILE.exists():
        return {}
    try:
        data = json.loads(STATE_FILE.read_text())
        if not isinstance(data, dict):
            return {}
        balance = data.get("balance")
        if balance is not None and not isinstance(balance, (int, float)):
            return {}
        return data
    except (json.JSONDecodeError, OSError):
        return {}


def save_state(balance: float):
    state = load_state()
    state["balance"] = balance
    try:
        with tempfile.NamedTemporaryFile(
            mode="w", dir=STATE_FILE.parent, delete=False, suffix=".tmp"
        ) as tmp:
            tmp_path = Path(tmp.name)
            json.dump(state, tmp, indent=2)
        tmp_path.chmod(0o600)
        tmp_path.replace(STATE_FILE)
    except OSError as e:
        console.print(f"[yellow]Warning: could not save state: {e}[/yellow]")


def calculate_month(balance: float, extra_payment: float, config: MortgageConfig) -> dict:
    balance_for_interest = balance - extra_payment
    interest = balance_for_interest * config.monthly_rate
    principal = config.monthly_payment - interest
    new_balance = balance - extra_payment - principal

    return {
        "starting_balance": balance,
        "extra_payment": extra_payment,
        "balance_for_interest": balance_for_interest,
        "interest": round(interest, 2),
        "principal": round(principal, 2),
        "total_paid": round(extra_payment + config.monthly_payment, 2),
        "new_balance": round(new_balance, 2),
    }


def project_months(
    starting_balance: float,
    monthly_extra: float,
    num_months: int,
    config: MortgageConfig,
) -> list:
    results = []
    balance = starting_balance

    for month in range(1, num_months + 1):
        extra = min(monthly_extra, balance - config.monthly_payment * 0.1)
        if extra < 0:
            extra = 0

        result = calculate_month(balance, extra, config)
        result["month"] = month
        results.append(result)

        balance = result["new_balance"]
        if balance <= 0:
            break

    return results


def print_single_month(result: dict, config: MortgageConfig):
    total_principal = result["extra_payment"] + result["principal"]

    table = Table(box=box.SIMPLE, show_header=False, padding=(0, 1))
    table.add_column(justify="left", style="dim")
    table.add_column(justify="right", style="bold")

    table.add_row("Starting balance", f"[cyan]${result['starting_balance']:>12,.2f}[/cyan]")
    table.add_row("Monthly rate", f"{config.monthly_rate * 100:.6f}%")
    table.add_section()
    table.add_row("Interest portion", f"[red]${result['interest']:>12,.2f}[/red]")
    table.add_row("Principal portion", f"${result['principal']:>12,.2f}")
    table.add_row("Regular payment", f"${config.monthly_payment:>12,.2f}")
    table.add_section()
    table.add_row("Extra payment", f"[yellow]${result['extra_payment']:>12,.2f}[/yellow]")
    table.add_row("Total principal paid", f"[green]${total_principal:>12,.2f}[/green]")
    table.add_row("Total paid this month", f"[green]${result['total_paid']:>12,.2f}[/green]")
    table.add_section()
    table.add_row("Remaining balance", f"[bold magenta]${result['new_balance']:>12,.2f}[/bold magenta]")

    console.print(Panel(table, title="[bold]Mortgage Payment Breakdown[/bold]", border_style="blue"))


def print_projection(results: list, config: MortgageConfig):
    table = Table(box=box.ROUNDED, show_footer=True)

    total_interest = sum(r["interest"] for r in results)
    total_principal = sum(r["principal"] for r in results)
    total_extra = sum(r["extra_payment"] for r in results)

    table.add_column("Month", justify="right", footer=f"[bold]{len(results)}[/bold]")
    table.add_column("Balance", justify="right", style="cyan", footer="")
    table.add_column("Extra", justify="right", style="yellow",
                     footer=f"[yellow]${total_extra:,.2f}[/yellow]")
    table.add_column("Interest", justify="right", style="red",
                     footer=f"[red]${total_interest:,.2f}[/red]")
    table.add_column("Principal", justify="right", footer=f"${total_principal:,.2f}")
    table.add_column("New Balance", justify="right", style="magenta",
                     footer=f"[bold magenta]${results[-1]['new_balance']:,.2f}[/bold magenta]")

    for r in results:
        table.add_row(
            str(r["month"]),
            f"${r['starting_balance']:,.2f}",
            f"${r['extra_payment']:,.2f}",
            f"${r['interest']:,.2f}",
            f"${r['principal']:,.2f}",
            f"${r['new_balance']:,.2f}",
        )

    console.print(Panel(table, title="[bold]Mortgage Projection[/bold]", border_style="blue"))

    total_payments = total_extra + len(results) * config.monthly_payment
    console.print(f"  Total payments:  [green]${total_payments:,.2f}[/green]")
    console.print(f"  Principal paid:  [green]${total_principal + total_extra:,.2f}[/green]")
    console.print(f"  Interest paid:   [red]${total_interest:,.2f}[/red]")

    if results[-1]["new_balance"] <= 0:
        console.print(f"\n  [bold green]Mortgage paid off in {len(results)} months![/bold green]")


def interactive_mode(config: MortgageConfig):
    state = load_state()
    saved_balance = state.get("balance")

    console.print(Panel(
        f"[bold]Mortgage Calculator[/bold]\n"
        f"Rate: [cyan]{config.annual_rate * 100:.2f}%[/cyan]  |  "
        f"Monthly payment: [cyan]${config.monthly_payment:,.2f}[/cyan]",
        border_style="blue"
    ))

    try:
        if saved_balance:
            console.print(f"  Saved balance: [cyan]${saved_balance:,.2f}[/cyan]")
            raw = input(f"  Current balance [${saved_balance:,.2f}]: ").strip()
            balance = float(raw) if raw else saved_balance
        else:
            balance = float(input("  Current balance: $"))
        _validate_amount(balance, "Balance")

        extra = float(input("  Extra payment this month: $"))
        _validate_amount(extra, "Extra payment")

        result = calculate_month(balance, extra, config)
        print_single_month(result, config)

        save_state(result["new_balance"])
        console.print(f"  [dim]Balance saved → ${result['new_balance']:,.2f}[/dim]\n")

        project = input("  Project future months? (y/n/payoff): ").strip().lower()
        if project == "y":
            months = int(input("  How many months? "))
            if not (1 <= months <= MAX_MONTHS):
                raise ValueError(f"Months must be between 1 and {MAX_MONTHS}")
            monthly_extra_raw = input(f"  Monthly extra payment [${extra:,.2f}]: ").strip()
            monthly_extra = float(monthly_extra_raw) if monthly_extra_raw else extra
            _validate_amount(monthly_extra, "Monthly extra payment")
            results = project_months(result["new_balance"], monthly_extra, months, config)
            print_projection(results, config)
        elif project in ("p", "payoff"):
            monthly_extra_raw = input(f"  Monthly extra payment [${extra:,.2f}]: ").strip()
            monthly_extra = float(monthly_extra_raw) if monthly_extra_raw else extra
            _validate_amount(monthly_extra, "Monthly extra payment")
            results = project_months(result["new_balance"], monthly_extra, MAX_MONTHS, config)
            print_projection(results, config)

    except ValueError as e:
        console.print(f"[red]Invalid input: {e}[/red]")
        sys.exit(1)
    except KeyboardInterrupt:
        console.print("\n[dim]Cancelled.[/dim]")
        sys.exit(0)


def main():
    parser = argparse.ArgumentParser(
        description="Calculate mortgage payments",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s                              Interactive mode (prompts for input)
  %(prog)s 17538                        Extra payment only — balance loaded from saved state
  %(prog)s 46534.45 17538               Balance + extra payment
  %(prog)s 46534.45 6000 -m 12          Project 12 months with $6000/month extra
  %(prog)s -p                           Project to full payoff from saved balance
  %(prog)s 5000 -p                      Project to payoff with $5000/month extra
  %(prog)s 27858.88 5000 -p             Project to payoff from given balance
  %(prog)s --rate 0.07 --payment 1500   Use a different mortgage configuration
        """
    )
    parser.add_argument("first", type=float, nargs="?",
                        help="Extra payment (if balance is saved) or current balance")
    parser.add_argument("second", type=float, nargs="?",
                        help="Extra payment (when balance given as first arg)")
    parser.add_argument("-m", "--months", type=int, default=1,
                        help="Number of months to project")
    parser.add_argument("-p", "--payoff", action="store_true",
                        help="Project all remaining months to full payoff")
    parser.add_argument("--rate", type=float, default=None,
                        help="Annual interest rate as decimal (e.g., 0.0649 for 6.49%%)")
    parser.add_argument("--payment", type=float, default=None,
                        help="Monthly payment amount in dollars")

    args = parser.parse_args()

    config = MortgageConfig(
        annual_rate=args.rate if args.rate is not None else DEFAULT_ANNUAL_RATE,
        monthly_payment=args.payment if args.payment is not None else DEFAULT_MONTHLY_PAYMENT,
    )

    try:
        config.validate()
        if args.first is not None:
            _validate_amount(args.first, "First argument")
        if args.second is not None:
            _validate_amount(args.second, "Second argument")
        if not (1 <= args.months <= MAX_MONTHS):
            raise ValueError(f"Months must be between 1 and {MAX_MONTHS}")
    except ValueError as e:
        console.print(f"[red]Invalid argument: {e}[/red]")
        sys.exit(1)

    if args.first is None and args.payoff:
        state = load_state()
        if not state.get("balance"):
            console.print("[red]No saved balance found. Provide a balance:[/red]")
            console.print("  mortgage-calculator.py 27858.88 --payoff")
            sys.exit(1)
        results = project_months(state["balance"], 0, MAX_MONTHS, config)
        print_projection(results, config)
        return

    if args.first is None:
        interactive_mode(config)
        return

    state = load_state()

    if args.second is not None:
        balance = args.first
        extra = args.second
    elif state.get("balance"):
        balance = state["balance"]
        extra = args.first
    else:
        console.print("[red]No saved balance found. Run with both balance and extra payment:[/red]")
        console.print("  mortgage-calculator.py 46534.45 17538")
        sys.exit(1)

    if args.payoff:
        results = project_months(balance, extra, MAX_MONTHS, config)
        print_projection(results, config)
    elif args.months > 1:
        results = project_months(balance, extra, args.months, config)
        print_projection(results, config)
    else:
        result = calculate_month(balance, extra, config)
        print_single_month(result, config)
        save_state(result["new_balance"])
        console.print(f"  [dim]Balance saved → ${result['new_balance']:,.2f}[/dim]\n")


if __name__ == "__main__":
    main()
