import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/*
 * 
 * clubs = 0
 * diamonds = 1
 * hearts = 2
 * spades = 3
 * 
 * status
 * active = 1
 * inactive = 0
 * current combo = 2
 * 
 * eliminated = -10
 * 
 */

public class PLBadugi500163559 implements PLBadugiPlayer {

	// 0 = turn off all
	// 1 = info only
	// 3 = minimal
	private static int DEBUG = 3;

	private int partialmatch = 0;
	private int firstQuad = 0;
	private int secondQuad = 0;
	private int thirdQuad = 0;

	// Betting variables.
	private float[] conditionThresholds = { 0.724388216825192f, 0.878108781974328f, 0.926911072f, 0.93661464f,
			0.98883368f };
	private float[] conditionThresholds1 = { 0.724388216825192f, 0.878108781974328f, 0.926911072f, 0.93661464f,
			0.98883368f };
	private float[] conditionThresholds2 = { 0.649389602f, 0.838880783f, 0.926911072f, 0.93661464f, 0.98883368f };
	private int betProfile = 0;
	private float aggro = 1;
	private boolean betCalculated = false;
	private boolean OppWaSi = false;
	private float iLosePc = 0;
	private float iDrawPc = 0;
	private float iWinPc = 0;
	private float evModifier = 1;
	private int lastEv = -1;
	private int betAmount = -1;
	private int netHandBetAllowance = 0;

	// Collecting statistics.
	private double oppBettingStr;
	private int beginningPot;
	private int timesOppRaised;
	private double handBettingStr;
	long startTime, endTime;

	// ArrayLists storages.
	private ArrayList<ArrayList<Integer>> master_odds = new ArrayList<ArrayList<Integer>>(270727);
	private ArrayList<Object[]> stats = new ArrayList<Object[]>();
	private ArrayList<ArrayList<Integer>> master_odds_index = new ArrayList<ArrayList<Integer>>(52);

	PrintWriter out = new PrintWriter(System.out);

	private String name;

	private static void message(PrintWriter out, String msg) {
		if (out != null && DEBUG == 1) {
			out.println(msg);
			out.flush();
		}
	}

	private static void message(PrintWriter out, String msg, int flags) {
		if (out != null && DEBUG > 0) {

			if (DEBUG == 1) {
				out.println(msg);
				out.flush();
			} else if (DEBUG == flags) {
				out.println(msg);
				out.flush();
			}

		}
	}

	private static void messagenobr(PrintWriter out, String msg) {
		if (out != null && DEBUG == 1) {
			out.print(msg);
			out.flush();
		}
	}

	// default constructor
	public PLBadugi500163559(String name) {
		this.name = name;
	}

	public PLBadugi500163559() {
		this.name = "Andy";
	}

	public void startNewMatch(int handsToGo) {
		startTime = System.nanoTime();
		messagenobr(out, " = LOAD NEW MATCH PARAMETERS... ");

		// https://crunchify.com/how-to-read-convert-csv-comma-separated-values-file-to-arraylist-in-java-using-split-operation/
		String fileToParse = "addon500163559.csv";

		BufferedReader crunchifyBuffer = null;

		try {
			String crunchifyLine;
			crunchifyBuffer = new BufferedReader(new FileReader(fileToParse));

			// reading CSV line by line
			while ((crunchifyLine = crunchifyBuffer.readLine()) != null) {
//				System.out.println("Raw CSV data: " + crunchifyLine);
//				System.out.println("Converted ArrayList data: " + crunchifyCSVtoArrayList(crunchifyLine));
				master_odds.add(crunchifyCSVtoArrayList(crunchifyLine));

			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (crunchifyBuffer != null)
					crunchifyBuffer.close();
			} catch (IOException crunchifyException) {
				crunchifyException.printStackTrace();
			}
		}

		message(out, " done.");

		message(out, " = LOAD NEW MATCH PARAMETERS... ", 1);
		buildIndex();
		message(out, " done.", 1);

		// check CSV integrity
//		checkCSVIntegrity2();

		partialmatch = Math.min((int) (handsToGo / 200), 250);
		firstQuad = Math.min((int) (handsToGo * 0.75), 25000);
		secondQuad = Math.min((int) (handsToGo * 0.5), 50000);
		thirdQuad = (int) (handsToGo * 0.25);

	}

	public void startNewHand(int position, int handsToGo, int currentScore) {

		// Reset after new drawing round.
		betCalculated = false;
		iLosePc = 0;
		iDrawPc = 0;
		iWinPc = 0;
		netHandBetAllowance = 0;

		// Reset stats.
		oppBettingStr = 0d;
		handBettingStr = 0d;
		beginningPot = 0;
		timesOppRaised = 0;

		OppWaSi = (position == 1) ? true : false;

		message(out, "** NEW HAND **");
//		hand_odds = master_odds;

		// resetting status
		for (ArrayList<Integer> row : master_odds) {
			row.set(9, 1);
		}
		if (partialmatch != 0 && (handsToGo % partialmatch) == 0) {
			endTime = System.nanoTime();
			float totalTime = (endTime - startTime) / 1000000000;
			startTime = endTime;
			message(out, handsToGo + ". [currentscore] " + currentScore + " [total]: " + totalTime, 3);
		}

		if (currentScore < 500 && handsToGo == firstQuad) {
			conditionThresholds = conditionThresholds2;
		}

	}

	public static ArrayList<Integer> crunchifyCSVtoArrayList(String crunchifyCSV) {
		ArrayList<Integer> crunchifyResult = new ArrayList<Integer>();

		if (crunchifyCSV != null) {
			String[] splitData = crunchifyCSV.split("\\s*,\\s*");
			for (int i = 0; i < splitData.length; i++) {
				if (!(splitData[i] == null) || !(splitData[i].length() == 0)) {
					crunchifyResult.add(Integer.parseInt(splitData[i].trim()));
				}
			}
		}

		return crunchifyResult;
	}

	// method to ask the agent what betting action it wants to perform
	public int bettingAction(int drawsRemaining, PLBadugiHand hand, int pot, int raises, int toCall, int minRaise,
			int maxRaise, int opponentDrew) {

		// Bet being calculated for the first time.
		if (!betCalculated) {
			betCalculated = true;

			// Stats reset
			beginningPot = pot;
			timesOppRaised = 0;

			// Removing hands and receiving report.
//			float[] report = removehand(hand);
			float[] report = removehand2(hand);

			message(out, "\t\t... " + report[0] * 100 + "% I lose (realized: " + report[3] * 100 + "%)");
//			message(out, "\t\t... " + report[1] * 100 + "% Draw (realized: " + report[4] * 100 + "%)");
			message(out, "\t\t..* " + report[2] * 100 + "% I win (realized: " + report[5] * 100 + "%)\t"
					+ (int) report[7] + " discards");

			// Determining the bet profile based on iWin % and games remaining.
			findBetProfile(drawsRemaining, opponentDrew, report);

			// Determined bet profile.
			message(out, "\t\t..* [BET Profile] >>> " + betProfile);
		}

//		message(out, " ***** $$$$$$$$ bettingAction *****");
		messagenobr(out, "\t [$$] [OppDrew=" + opponentDrew + " cards  ");
		messagenobr(out, "Raised=" + raises + "]\t");
		messagenobr(out, "[pot=" + pot + "  ");
		messagenobr(out, "call=" + toCall + "  ");
		messagenobr(out, "raiseRng " + minRaise + ".." + maxRaise + "]");
		messagenobr(out, "\n");
//		message(out, "\t\t... [BET EV] >>> " + (int) Math.round(expectvalue(iLosePc, iDrawPc, iWinPc, pot + toCall))
//				+ " (" + expectvalue(iLosePc, iDrawPc, iWinPc, pot + toCall) + ")");

		betAmount = findBetAmount(betProfile, (int) Math.round(expectvalue(iLosePc, iDrawPc, iWinPc, pot + toCall)),
				pot, raises, toCall, minRaise, maxRaise, OppWaSi);

		oppBettingStr += ((double) toCall / (double) beginningPot);
		message(out,
				"\tbeginning pot " + beginningPot + "\t[bet strength] " + oppBettingStr + " times: " + timesOppRaised,
				1);
		if (toCall != 0)
			timesOppRaised++;

		return betAmount;

	}

	private int findBetAmount(int betProfile, int ev, int pot, int raises, int toCall, int minRaise, int maxRaise,
			boolean OppWaSi) {
		int result = 0;
		int tens = (int) Math.floor(betProfile / 10);
		boolean mefirst = (raises == 0) ? true : false;
		boolean raisedonce = (raises == 1 || raises == 2) ? true : false;
		boolean raisedtwice = (raises == 3 || raises >= 4) ? true : false;
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		int singles = betProfile % 10;

		if (lastEv == -1 || raises == 0 || raises == 1)
//			lastEv = ev;
			lastEv = ev + netHandBetAllowance;

		message(out, " \t\t ... [LAST EV] " + lastEv + "\t[current EV] " + ev, 2);

		switch (tens) {
		case 0:
			// 0 last round
			if (singles == 1) {
				double r = rand.nextDouble(0.2, 0.3);
				if (mefirst) {
					result = rand.nextInt(minRaise, minRaise + 1 + (int) Math.round(r));
					lastEv = lastEv - result;
				} else {
					if (toCall > lastEv * r && toCall > 6) {
						result = 0;
					} else {
						result = toCall;
					}
				}

			}
			if (singles == 2) {
				double r = rand.nextDouble(0.2, 0.3);
				if (mefirst) {
					result = rand.nextInt(minRaise, minRaise + 1 + (int) Math.round(r) * minRaise);
					lastEv = lastEv - result;
				} else {
					result = toCall;
				}

			}

			if (singles == 3) {
				double r = rand.nextDouble(0.2, 0.3);
				if (mefirst) {
					result = (int) (ev * 0.724388 * r);
					lastEv = lastEv - result;
				} else if (raisedonce || raisedtwice) {
					result = toCall;
					lastEv = lastEv - result;
				}
			}

			if (singles == 4) {
				double r = rand.nextDouble(0.3, 0.6);
				if (mefirst) {
					result = (int) (ev * 0.878108 * r);
					lastEv = lastEv - result;
				} else if (raisedonce || raisedtwice) {
					if (toCall < lastEv) {
						result = toCall;
					} else {
						result = toCall;
						message(out, "** WARNING: over the betting limit**", 2);
					}
				}
			}

			if (singles == 5) {
				double r = rand.nextDouble(0.8, 0.9);
				if (mefirst) {
					result = (int) (ev * 0.724388 * r);
					lastEv = lastEv - result;
				} else if (raisedonce) {
					result = (int) (ev * 0.878108 * r);
					lastEv = ev - result;
				} else if (raisedtwice) {
//					result = toCall;
					result = (int) (ev * 0.878108 * r);
					lastEv = lastEv - result;
				}
			}

			if (singles == 6) {
				double r = rand.nextDouble(0.9, 1.1);
				if (mefirst) {
					result = (int) (ev * 0.724388 * r);
					lastEv = lastEv - result;
				} else if (raisedonce) {
					result = (int) (ev * 0.878108 * r);
					lastEv = ev - result;
				} else if (raisedtwice) {
//					result = (int) (ev * 0.926911 * r);
					result = toCall;
					lastEv = lastEv - result;
				}
			}

			break;

		case 1:
		case 2:
			// 2
			if (singles == 1 || singles == 2) {
				double r = (singles == 1) ? rand.nextDouble(0.2, 0.3) : rand.nextDouble(0.3, 0.6);
				if (mefirst) {
					result = rand.nextInt(minRaise, minRaise + 1 + (int) Math.round(minRaise * 0.2));
					lastEv = lastEv - result;
				} else {
					if (toCall > lastEv * r && toCall > 8 && (netHandBetAllowance + ev) > toCall) {
						result = 0;
					} else {
						result = toCall;
						lastEv = lastEv - result;
					}
				}

			}

			if (singles == 3) {
				double r = rand.nextDouble(0.2, 0.3);
				if (mefirst) {
					result = (int) (ev * 0.724388 * r);
					lastEv = lastEv - result;
				} else if (raisedonce || raisedtwice) {
					result = toCall;
					lastEv = lastEv - result;
				}
			}

			if (singles == 4) {
				double r = rand.nextDouble(0.3, 0.6);
				if (mefirst) {
					result = (int) (ev * 0.878108 * r);
					lastEv = lastEv - result;
				} else if (raisedonce || raisedtwice) {
					if (toCall < lastEv) {
						result = toCall;
					} else {
						result = toCall;
						message(out, "** WARNING: over the betting limit**", 2);
					}
				}
			}

			if (singles == 5) {
				double r = rand.nextDouble(0.8, 0.9);
				if (mefirst) {
					result = (int) (ev * 0.724388 * r);
					lastEv = lastEv - result;
				} else if (raisedonce) {
					result = (int) (ev * 0.878108 * r);
					lastEv = ev - result;
				} else if (raisedtwice) {
//					result = toCall;
					result = (int) (ev * 0.878108 * r);
					lastEv = lastEv - result;
				}
			}

			if (singles == 6) {
				double r = rand.nextDouble(0.9, 1.1);
				if (mefirst) {
					result = (int) (ev * 0.724388 * r);
					lastEv = lastEv - result;
				} else if (raisedonce) {
					result = (int) (ev * 0.878108 * r);
					lastEv = ev - result;
				} else if (raisedtwice) {
					result = (int) (ev * 0.926911 * r);
					lastEv = lastEv - result;
				}
			}

			break;

		case 3:

			// 1 and 2
			if (singles == 1 || singles == 2) {
				if (mefirst) {
					result = rand.nextInt(minRaise, minRaise + 1);
					lastEv = lastEv - result;
				} else {

					result = toCall;
					lastEv = lastEv - result;
				}

			}

			if (singles == 3) {
				if (mefirst) {
					result = (int) (ev * 0.724388);
					lastEv = lastEv - result;
				} else if (raisedonce || raisedtwice) {
					result = toCall;
					lastEv = lastEv - result;
				}
			}

			if (singles == 4) {
				double r = rand.nextDouble(0.2, 0.3);
				if (mefirst) {
					result = (int) (ev * 0.878108 * r);
					lastEv = lastEv - result;
				} else if (raisedonce || raisedtwice) {
					if (toCall < lastEv) {
						result = toCall;
					} else {
						result = toCall;
						message(out, "** WARNING: over the betting limit**", 2);
					}
				}
			}

			if (singles == 5) {
				double r = rand.nextDouble(0.8, 0.9);
				if (mefirst) {
					result = (int) (ev * 0.724388 * r);
					lastEv = lastEv - result;
				} else if (raisedonce) {
					result = (int) (ev * 0.878108 * r);
					lastEv = ev - result;
				} else if (raisedtwice) {
					result = toCall;
					lastEv = lastEv - result;
				}
			}

			if (singles == 6) {
				double r = rand.nextDouble(0.9, 1.1);
				if (mefirst) {
					result = (int) (ev * 0.724388 * r);
					lastEv = lastEv - result;
				} else if (raisedonce) {
//					result = (int) (ev * 0.878108 * r);
					result = toCall;
					lastEv = ev - result;
				} else if (raisedtwice) {
//					result = (int) (ev * 0.926911 * r);
					result = toCall;
					lastEv = lastEv - result;
				}
			}

			break;
		}

		netHandBetAllowance += lastEv;
		message(out, " \t\t\t ... [Bet] " + result + "\t" + " [netHandBetAllowance] " + netHandBetAllowance
				+ "\t[Remaining EV allowed] " + lastEv, 2);
		return result;

	}

	/**
	 * Based on report and number of cards opponent drawn last round, find bet
	 * profile.
	 */
	private void findBetProfile(int drawsRemaining, int opponentDrew, float[] report) {
		switch (drawsRemaining) {
		case 3:
			// 3 Games Remaining
			// 33333333333333333333333333333333333333333
			iLosePc = report[0];
			iDrawPc = report[1];
			iWinPc = report[2];

			if (iWinPc < conditionThresholds[0]) {
				betProfile = 31;
			} else if (iWinPc < conditionThresholds[1]) {
				betProfile = 32;
			} else if (iWinPc < conditionThresholds[2]) {
				betProfile = 33;
			} else if (iWinPc < conditionThresholds[3]) {
				betProfile = 34;
			} else if (iWinPc < conditionThresholds[4]) {
				betProfile = 35;
			} else {
				betProfile = 36;
			}
			break;
		case 2:
			// 2 Games Remaining
			// 22222222222222222222222222222222222222222

			// [FUTURE FEATURE] can include randomizer
			// Java.lang.math.min()
			// Math.min(iWinPc, iWinPcR) + (Math.abs(iWinPcR - iWinPC)*rand)

			// Determine which set of probability based on opponent's discarding habits.
			if (opponentDrew >= 3) {
				// Discarded 3 or 4
				iLosePc = report[0];
				iDrawPc = report[1];
				iWinPc = report[2];

				if (iWinPc < conditionThresholds[0]) {
					betProfile = 21;
				} else if (iWinPc < conditionThresholds[1]) {
					betProfile = 22;
				} else if (iWinPc < conditionThresholds[2]) {
					betProfile = 23;
				} else if (iWinPc < conditionThresholds[3]) {
					betProfile = 24;
				} else if (iWinPc < conditionThresholds[4]) {
					betProfile = 25;
				} else {
					betProfile = 26;
				}
			} else if (opponentDrew == 2) {
				// Discarded 2
				iLosePc = report[0];
				iDrawPc = report[1];
				iWinPc = report[2];

				if (iWinPc < conditionThresholds[0]) {
					betProfile = 21;
				} else if (iWinPc < conditionThresholds[1]) {
					betProfile = 22;
				} else if (iWinPc < conditionThresholds[2]) {
					betProfile = 22;
				} else if (iWinPc < conditionThresholds[3]) {
					betProfile = 23;
				} else if (iWinPc < conditionThresholds[4]) {
					betProfile = 24;
				} else {
					betProfile = 25;
				}
			} else if (opponentDrew == 1) {
				// Discarded 1.
				iLosePc = (report[0] + report[3]) / 2;
				iDrawPc = (report[1] + report[4]) / 2;
				iWinPc = (report[2] + report[5]) / 2;

				if (iWinPc < conditionThresholds[0]) {
					betProfile = 21;
				} else if (iWinPc < conditionThresholds[1]) {
					betProfile = 21;
				} else if (iWinPc < conditionThresholds[2]) {
					betProfile = 22;
				} else if (iWinPc < conditionThresholds[3]) {
					betProfile = 22;
				} else if (iWinPc < conditionThresholds[4]) {
					betProfile = 23;
				} else {
					betProfile = 24;
				}
			} else {
				// Discarded none.
				iLosePc = report[3];
				iDrawPc = report[4];
				iWinPc = report[5];

				if (iWinPc < conditionThresholds[0]) {
					betProfile = 21;
				} else if (iWinPc < conditionThresholds[1]) {
					betProfile = 21;
				} else if (iWinPc < conditionThresholds[2]) {
					betProfile = 21;
				} else if (iWinPc < conditionThresholds[3]) {
					betProfile = 21;
				} else if (iWinPc < conditionThresholds[4]) {
					betProfile = 22;
				} else {
					betProfile = 23;
				}
			}

			break;
		case 1:
			// 1 game Remaining
			// 11111111111111111111111111111111111

			// Determine which set of probability based on opponent's discarding habits.
			if (opponentDrew >= 3) {
				// Discarded 3 or 4
				iLosePc = report[0];
				iDrawPc = report[1];
				iWinPc = report[2];

				if (iWinPc < conditionThresholds[0]) {
					betProfile = 11;
				} else if (iWinPc < conditionThresholds[1]) {
					betProfile = 12;
				} else if (iWinPc < conditionThresholds[2]) {
					betProfile = 13;
				} else if (iWinPc < conditionThresholds[3]) {
					betProfile = 14;
				} else if (iWinPc < conditionThresholds[4]) {
					betProfile = 15;
				} else {
					betProfile = 16;
				}
			} else if (opponentDrew == 2) {
				// Discarded 2
				iLosePc = (report[0] + report[3]) / 2;
				iDrawPc = (report[1] + report[4]) / 2;
				iWinPc = (report[2] + report[5]) / 2;

				if (iWinPc < conditionThresholds[0]) {
					betProfile = 11;
				} else if (iWinPc < conditionThresholds[1]) {
					betProfile = 12;
				} else if (iWinPc < conditionThresholds[2]) {
					betProfile = 12;
				} else if (iWinPc < conditionThresholds[3]) {
					betProfile = 13;
				} else if (iWinPc < conditionThresholds[4]) {
					betProfile = 14;
				} else {
					betProfile = 15;
				}
			} else if (opponentDrew == 1) {
				// Discarded 1.
				iLosePc = report[3];
				iDrawPc = report[4];
				iWinPc = report[5];

				if (iWinPc < conditionThresholds[0]) {
					betProfile = 11;
				} else if (iWinPc < conditionThresholds[1]) {
					betProfile = 11;
				} else if (iWinPc < conditionThresholds[2]) {
					betProfile = 12;
				} else if (iWinPc < conditionThresholds[3]) {
					betProfile = 12;
				} else if (iWinPc < conditionThresholds[4]) {
					betProfile = 13;
				} else {
//					betProfile = 14;
					betProfile = 15;
				}
			} else {
				// Discarded none.
				iLosePc = report[3];
				iDrawPc = report[4];
				iWinPc = report[5];

				if (iWinPc < conditionThresholds[0]) {
					betProfile = 11;
				} else if (iWinPc < conditionThresholds[1]) {
					betProfile = 11;
				} else if (iWinPc < conditionThresholds[2]) {
					betProfile = 11;
				} else if (iWinPc < conditionThresholds[3]) {
					betProfile = 11;
				} else if (iWinPc < conditionThresholds[4]) {
					betProfile = 12;
				} else {
//					betProfile = 13;
					betProfile = 14;
				}
			}

			break;
		case 0:
			// No game Remaining
			// 000000000000000000000000000000000000

			// Determine which set of probability based on opponent's discarding habits.
			if (opponentDrew >= 3) {
				// Discarded 3 or 4
				iLosePc = (report[0] + report[3]) / 2;
				iDrawPc = (report[1] + report[4]) / 2;
				iWinPc = (report[2] + report[5]) / 2;

				if (iWinPc < conditionThresholds[0]) {
					betProfile = 1;
				} else if (iWinPc < conditionThresholds[1]) {
					betProfile = 2;
				} else if (iWinPc < conditionThresholds[2]) {
					betProfile = 3;
				} else if (iWinPc < conditionThresholds[3]) {
					betProfile = 4;
				} else if (iWinPc < conditionThresholds[4]) {
					betProfile = 5;
				} else {
					betProfile = 6;
				}
			} else if (opponentDrew == 2) {
				// Discarded 2
				iLosePc = report[3];
				iDrawPc = report[4];
				iWinPc = report[5];

				if (iWinPc < conditionThresholds[0]) {
					betProfile = 1;
				} else if (iWinPc < conditionThresholds[1]) {
					betProfile = 2;
				} else if (iWinPc < conditionThresholds[2]) {
					betProfile = 2;
				} else if (iWinPc < conditionThresholds[3]) {
					betProfile = 3;
				} else if (iWinPc < conditionThresholds[4]) {
					betProfile = 4;
				} else {
					betProfile = 5;
				}
			} else if (opponentDrew == 1) {
				// Discarded 1.
				iLosePc = report[3];
				iDrawPc = report[4];
				iWinPc = report[5];

				if (iWinPc < conditionThresholds[0]) {
					betProfile = 1;
				} else if (iWinPc < conditionThresholds[1]) {
					betProfile = 1;
				} else if (iWinPc < conditionThresholds[2]) {
					betProfile = 2;
				} else if (iWinPc < conditionThresholds[3]) {
					betProfile = 2;
				} else if (iWinPc < conditionThresholds[4]) {
					betProfile = 3;
				} else {
					betProfile = 4;
				}
			} else {
				// Discarded none.
				iLosePc = report[3];
				iDrawPc = report[4];
				iWinPc = report[5];

				if (iWinPc < conditionThresholds[0]) {
					betProfile = 1;
				} else if (iWinPc < conditionThresholds[1]) {
					betProfile = 1;
				} else if (iWinPc < conditionThresholds[2]) {
					betProfile = 1;
				} else if (iWinPc < conditionThresholds[3]) {
					betProfile = 1;
				} else if (iWinPc < conditionThresholds[4]) {
					betProfile = 2;
				} else {
					betProfile = 3;
				}
			}

			break;
		}
	}

	// Collecting stats
	private void addstats(ArrayList<Object[]> s, Float better, Float worse) {
		/*
		 * 
		 * 
		 * ML for Opponent Op has better hand Op has worse hand Op's bets relative to
		 * pot Op no of times average reraising 1 (reraise1*(1/(d+1))/pot +
		 * (reraise2*(1/(d+1))/pot + (reraiseN*(1/(d+1))/pot) / N Op no of cards
		 * discarding 1 Op no of times average reraising 2 Op no of cards discarding 3
		 * Op no of times average reraising 4 Op no of cards discarding 5 payout in +/-
		 * 
		 * 
		 * 
		 */
	}

	// Method to determine what card to replace.
	public List<Card> drawingAction(int drawsRemaining, PLBadugiHand hand, int pot, int dealerDrew) {
		// Reset after new drawing round.
		betCalculated = false;
		iLosePc = 0;
		iDrawPc = 0;
		iWinPc = 0;
		betAmount = -1;
//		iWinPcR = 0;

		List<Card> allCards = hand.getAllCards();
		List<Card> activeCards = hand.getActiveCards();
		List<Card> inactiveCards = hand.getInactiveCards();
		int[] powerOfCards = hand.getActiveRanks();

		// list of cards discarding
		List<Card> discard = new ArrayList<Card>();

		handBettingStr += (timesOppRaised != 0) ? oppBettingStr / timesOppRaised : 0;
		message(out, "\t[draw bet str] " + handBettingStr, 1);

		message(out, " ------------------------------------------------------------ " + drawsRemaining
				+ " draws remain \t dealerDrew: " + drawsRemaining + "");

		/*
		 * CONDITIONS for card replacements - queen and king on the first 2 rounds
		 */

		for (Card c : allCards) {
			// always add inactive cards to retry

			// if it has 3 rounds, and badugi, discard King only if it exists, then break
			if (drawsRemaining == 3 && inactiveCards.size() == 0) {
				if (c.getRank() == 13)
					discard.add(c);
				break;
			}

			// in any other rounds and badugi, break
			if (drawsRemaining != 3 && inactiveCards.size() == 0)
				break;

			if (inactiveCards.contains(c)) {
				discard.add(c);
				continue;
			}

			// round 1
			if ((drawsRemaining == 3 && c.getRank() >= 10 && inactiveCards.size() != 0)) {
				discard.add(c);
				continue;
			}

			// round 2
			if ((drawsRemaining == 2 && c.getRank() > 11 && inactiveCards.size() != 0)) {
				discard.add(c);
				continue;
			}
		}

		return discard;

	}

	// method that gets called at the end of the current hand, whether fold or show
	// down
	public void handComplete(PLBadugiHand yourHand, PLBadugiHand opponentHand, int result) {
		messagenobr(out, " [handComplete()] $" + result);
		messagenobr(out, " me> " + yourHand.toString());
		if (opponentHand != null) {
			messagenobr(out, " -- opp> " + opponentHand.toString());
		} else {
			messagenobr(out, " -- opp> *FOLDED* ");
		}
		messagenobr(out, "\n\n");

		if (opponentHand != null) {
			String s = "r\t" + result + "\t" + "bStr\t" + (float) (handBettingStr / 4) + "\t" + "mh\t"
					+ Arrays.toString(yourHand.getActiveRanks()) + "\t" + "oh\t"
					+ Arrays.toString(opponentHand.getActiveRanks());

			message(out, s, 4);
			messagenobr(out, "\n\n");
		}

		message(out,
				"#########################################################################################################");
		message(out,
				"#########################################################################################################\n\n");

	}

	// Linear Equation for Power of 3.
	public float linEq(float x) {
		return (float) ((-0.0055) * (Math.pow((double) x, 2)) + (0.123) * x + 0.2463);
	}

	public String getAgentName() {
		return name;
	}

	public String getAuthor() {
		return "Lee, Andy";
	}

	// Brute force check CSV integrity
	public void checkCSVIntegrity() {
		message(out, " = CHECKING CSV INTEGRITY =============== ");

		int currentidx = 0;

		for (ArrayList<Integer> row : master_odds) {

			String hand = new String();
			hand = "";

			for (int i = 0; i <= 7; i++) {
				if ((i % 2) == 0) {
					switch (row.get(i)) {
					case 13:
						hand = hand + "k";
						break;
					case 12:
						hand = hand + "q";
						break;
					case 11:
						hand = hand + "j";
						break;
					case 10:
						hand = hand + "t";
						break;
					case 9:
						hand = hand + "9";
						break;
					case 8:
						hand = hand + "8";
						break;
					case 7:
						hand = hand + "7";
						break;
					case 6:
						hand = hand + "6";
						break;
					case 5:
						hand = hand + "5";
						break;
					case 4:
						hand = hand + "4";
						break;
					case 3:
						hand = hand + "3";
						break;
					case 2:
						hand = hand + "2";
						break;
					case 1:
						hand = hand + "a";
						break;
					default:
						hand = hand + row.get(i);
						break;
					}
				} else {
					switch (row.get(i)) {
					case 3:
						hand = hand + "s";
						break;
					case 2:
						hand = hand + "h";
						break;
					case 1:
						hand = hand + "d";
						break;
					case 0:
						hand = hand + "c";
						break;
					}

				}

			}

			PLBadugiHand thisHand = new PLBadugiHand(hand);

			currentidx++;
			message(out, "" + currentidx);

			for (ArrayList<Integer> row2 : master_odds.subList(currentidx + 1, master_odds.size())) {

				String hand2 = new String();
				hand2 = "";

				for (int i = 0; i <= 7; i++) {
					if ((i % 2) == 0) {
						switch (row2.get(i)) {
						case 13:
							hand2 = hand2 + "k";
							break;
						case 12:
							hand2 = hand2 + "q";
							break;
						case 11:
							hand2 = hand2 + "j";
							break;
						case 10:
							hand2 = hand2 + "t";
							break;
						case 9:
							hand2 = hand2 + "9";
							break;
						case 8:
							hand2 = hand2 + "8";
							break;
						case 7:
							hand2 = hand2 + "7";
							break;
						case 6:
							hand2 = hand2 + "6";
							break;
						case 5:
							hand2 = hand2 + "5";
							break;
						case 4:
							hand2 = hand2 + "4";
							break;
						case 3:
							hand2 = hand2 + "3";
							break;
						case 2:
							hand2 = hand2 + "2";
							break;
						case 1:
							hand2 = hand2 + "a";
							break;
						default:
							hand2 = hand2 + row2.get(i);
							break;
						}
					} else {
						switch (row2.get(i)) {
						case 3:
							hand2 = hand2 + "s";
							break;
						case 2:
							hand2 = hand2 + "h";
							break;
						case 1:
							hand2 = hand2 + "d";
							break;
						case 0:
							hand2 = hand2 + "c";
							break;
						}

					}

				}

				PLBadugiHand otherHand = new PLBadugiHand(hand2);

				if (thisHand.compareTo(otherHand) > 0) {
					message(out, "[ERROR] " + row.toString() + " " + thisHand.toString() + " -- " + row2.toString()
							+ " " + otherHand.toString());
				} else {
//		    		message(out,"success. " + thisHand.toString() + " " + otherHand.toString() );
				}

			}

		}

		message(out, " = DONE =============== ");
	}

	// Quick check CSV integrity
	public void checkCSVIntegrity2() {
		message(out, " = CHECKING CSV INTEGRITY =============== ");

//		for (ArrayList<Integer> row : master_odds) {
		for (int x = 1; x < master_odds.size(); x++) {

			ArrayList<Integer> row = master_odds.get(x - 1);
			ArrayList<Integer> row2 = master_odds.get(x);

			String hand = "";
			String hand2 = "";

			for (int i = 0; i <= 7; i++) {
				if ((i % 2) == 0) {
					switch (row.get(i)) {
					case 13:
						hand = hand + "k";
						break;
					case 12:
						hand = hand + "q";
						break;
					case 11:
						hand = hand + "j";
						break;
					case 10:
						hand = hand + "t";
						break;
					case 9:
						hand = hand + "9";
						break;
					case 8:
						hand = hand + "8";
						break;
					case 7:
						hand = hand + "7";
						break;
					case 6:
						hand = hand + "6";
						break;
					case 5:
						hand = hand + "5";
						break;
					case 4:
						hand = hand + "4";
						break;
					case 3:
						hand = hand + "3";
						break;
					case 2:
						hand = hand + "2";
						break;
					case 1:
						hand = hand + "a";
						break;
					default:
						hand = hand + row.get(i);
						break;
					}
				} else {
					switch (row.get(i)) {
					case 3:
						hand = hand + "s";
						break;
					case 2:
						hand = hand + "h";
						break;
					case 1:
						hand = hand + "d";
						break;
					case 0:
						hand = hand + "c";
						break;
					}

				}
			}

//				message(out, "" + x);

			for (int j = 0; j <= 7; j++) {
				if ((j % 2) == 0) {
					switch (row2.get(j)) {
					case 13:
						hand2 = hand2 + "k";
						break;
					case 12:
						hand2 = hand2 + "q";
						break;
					case 11:
						hand2 = hand2 + "j";
						break;
					case 10:
						hand2 = hand2 + "t";
						break;
					case 9:
						hand2 = hand2 + "9";
						break;
					case 8:
						hand2 = hand2 + "8";
						break;
					case 7:
						hand2 = hand2 + "7";
						break;
					case 6:
						hand2 = hand2 + "6";
						break;
					case 5:
						hand2 = hand2 + "5";
						break;
					case 4:
						hand2 = hand2 + "4";
						break;
					case 3:
						hand2 = hand2 + "3";
						break;
					case 2:
						hand2 = hand2 + "2";
						break;
					case 1:
						hand2 = hand2 + "a";
						break;
					default:
						hand2 = hand2 + row2.get(j);
						break;
					}
				} else {
					switch (row2.get(j)) {
					case 3:
						hand2 = hand2 + "s";
						break;
					case 2:
						hand2 = hand2 + "h";
						break;
					case 1:
						hand2 = hand2 + "d";
						break;
					case 0:
						hand2 = hand2 + "c";
						break;
					}

				}
			}

			PLBadugiHand smallHand = new PLBadugiHand(hand);
			PLBadugiHand largeHand = new PLBadugiHand(hand2);

			int compareHand = smallHand.compareTo(largeHand);

			if (compareHand > 0) {
				message(out,
						"[ERROR] [" + compareHand + "] " + row.subList(0, 9).toString() + " smallHand:"
								+ smallHand.toString() + " -- " + row2.subList(0, 9).toString() + " largeHand"
								+ largeHand.toString());
			} else {
				if (x % 25000 == 0) {
					messagenobr(out, x + ".. ");
				}
//					message(out, "success. " + row.subList(8, 9).toString() + " " + thisHand.toString() + " -- "
//							+ row2.subList(8, 9).toString() + " " + otherHand.toString());
			}

		}

	}

	// Main function for removing seen cards from the masterlist
	public float[] removehand(PLBadugiHand hand) {
		// mark hand to 2 if it finds hand
		boolean foundMatch = false;
		int counter1 = 0;
		int counter_marked = 0;
		int currentHandScore = 0;

		int r1 = hand.getAllCards().get(0).getRank();
		int s1 = hand.getAllCards().get(0).getSuit();
		int r2 = hand.getAllCards().get(1).getRank();
		int s2 = hand.getAllCards().get(1).getSuit();
		int r3 = hand.getAllCards().get(2).getRank();
		int s3 = hand.getAllCards().get(2).getSuit();
		int r4 = hand.getAllCards().get(3).getRank();
		int s4 = hand.getAllCards().get(3).getSuit();

//    	message(out,"\t... finding match => [" + r1 + ", "+ s1 + ", "+ r2 + ", "+ s2 + ", "+ r3 + ", "+ s3 + ", "+ r4 + ", "+ s4 + "]");
		for (ArrayList<Integer> row : master_odds) {
//    		message(out," ... match " + row.toString() + " => [" + r1 + ", "+ s1 + ", "+ r2 + ", "+ s2 + ", "+ r3 + ", "+ s3 + ", "+ r4 + ", "+ s4 + "]");
			counter1++;

			/*
			 * [9] = 2 // current hand [9] = 1 // hand still exist [9] = 0 // hand DNE
			 * 
			 */

			// finding perfect match
			if (foundMatch == false && row.get(0).intValue() == r1 && row.get(1).intValue() == s1
					&& row.get(2).intValue() == r2 && row.get(3).intValue() == s2 && row.get(4).intValue() == r3
					&& row.get(5).intValue() == s3 && row.get(6).intValue() == r4 && row.get(7).intValue() == s4) {

				row.set(9, 2);
				currentHandScore = row.get(8);
//    			message(out,"\t\t... Success. status = 2 ");
				foundMatch = true;
				continue;
			}

			// mark cards contained in hand to -1
			if (row.get(9) != 2 || row.get(9) != -1) {
				if ((row.get(0) == r1 && row.get(1) == s1) || (row.get(2) == r1 && row.get(3) == s1)
						|| (row.get(4) == r1 && row.get(5) == s1) || (row.get(6) == r1 && row.get(7) == s1)
						|| (row.get(0) == r2 && row.get(1) == s2) || (row.get(2) == r2 && row.get(3) == s2)
						|| (row.get(4) == r2 && row.get(5) == s2) || (row.get(6) == r2 && row.get(7) == s2)
						|| (row.get(0) == r3 && row.get(1) == s3) || (row.get(2) == r3 && row.get(3) == s3)
						|| (row.get(4) == r3 && row.get(5) == s3) || (row.get(6) == r3 && row.get(7) == s3)
						|| (row.get(0) == r4 && row.get(1) == s4) || (row.get(2) == r4 && row.get(3) == s4)
						|| (row.get(4) == r4 && row.get(5) == s4) || (row.get(6) == r4 && row.get(7) == s4)) {
//    				message(out,"    ... status = 0 ");
					row.set(9, 0);
					counter_marked++;
					continue;
				}
			}
		}

		// Last alert if complete match not found.
		if (!foundMatch) {
			message(out, " ... aiyayo, match NOT FOUND! => [" + r1 + ", " + s1 + ", " + r2 + ", " + s2 + ", " + r3
					+ ", " + s3 + ", " + r4 + ", " + s4 + "]");
		}

		// Calculations
		// Initialize variables.
		int numGtCurrentScore = 0;
		int numEqCurrentScore = 0;
		int numLtCurrentScore = 0;
		int noOfdiscarded = 0;

		// tally only hands that are at least power of 3
		int numLtCurrentScoreIgnoring1and2 = 0;
		int numEqCurrentScoreIgnoring1and2 = 0;
		int numGtCurrentScoreIgnoring1and2 = 0;
		int numOfdiscarded1and2 = 99125;

		// [0,1],[2,3],[4,5],[6,7] cards
		// [8] absolute rank
		// [9] card active in deck
		// [10] power of card

//    	message(out,"\t... calculating odds");
		for (ArrayList<Integer> row : master_odds) {
			switch (row.get(9)) {
			case 0:
				noOfdiscarded++;
				if (row.get(10) <= 2)
					numOfdiscarded1and2--;
				break;
			case 1:
				if (currentHandScore < row.get(8)) {
					// number greater than current score (i lose)
					numGtCurrentScore++;
					if (row.get(10) > 2)
						numGtCurrentScoreIgnoring1and2++;

				} else if (currentHandScore == row.get(8)) {
					// number equal current score (we draw)
					numEqCurrentScore++;
					if (row.get(10) > 2)
						numEqCurrentScoreIgnoring1and2++;

				} else {
					// number less than current score (i win)
					numLtCurrentScore++;
					if (row.get(10) > 2)
						numLtCurrentScoreIgnoring1and2++;

				}
				break;
			case 2:
//    			message(out,"\t\t... found status = 2.");
				break;
			}
		}

		float iLose = ((float) numGtCurrentScore / (270725 - noOfdiscarded));
		float iDraw = ((float) numEqCurrentScore / (270725 - noOfdiscarded));
		float iWin = ((float) numLtCurrentScore / (270725 - noOfdiscarded));
//		float numDcPercentage = ((float) noOfdiscarded / (270725) );

		float iLoseRealized = ((float) numGtCurrentScoreIgnoring1and2 / (270725 - numOfdiscarded1and2 - noOfdiscarded));
		float iDrawRealized = ((float) numEqCurrentScoreIgnoring1and2 / (270725 - numOfdiscarded1and2 - noOfdiscarded));
		float iWinRealized = ((float) numLtCurrentScoreIgnoring1and2 / (270725 - numOfdiscarded1and2 - noOfdiscarded));

//		message(out, "\t\t... " + iLose + "% I lose");
//		message(out, "\t\t... " + iWin + "% I win");

		if ((numGtCurrentScore + numEqCurrentScore + numLtCurrentScore + noOfdiscarded + 1) != 270725) {
			message(out, "\t\t... 270725 mismatch. Total: "
					+ (numGtCurrentScore + numEqCurrentScore + numLtCurrentScore + noOfdiscarded + 1));
		}

		float[] report = new float[8];
		report[0] = iLose;
		report[1] = iDraw;
		report[2] = iWin;
		report[3] = iLoseRealized;
		report[4] = iDrawRealized;
		report[5] = iWinRealized;
		report[7] = noOfdiscarded;

		return report;

	}

	// Finding expected value.
	public float expectvalue(float ilosepc, float idrawpc, float iwinpc, int expectedpot) {

		float result = iwinpc * expectedpot + 0 * ilosepc + idrawpc * (expectedpot / 2);

		result = result * evModifier;

		return result;
	}

	// Convert card to 0-51 value. Used for indexing
	public int convertCardIndex(int rank, int suit) {
		return (rank - 1) * 4 + (suit);
	}

	// Builds an index for each card, of the card location in master_odds array
	public void buildIndex() {
		for (int row = 0; row < 52; row++) {
			master_odds_index.add(row, new ArrayList<Integer>());
		}

		for (int row = 0; row < master_odds.size(); row++) {
			for (int i = 0; i < 4; i++) {
				int r = master_odds.get(row).get(i * 2);
				int s = master_odds.get(row).get(i * 2 + 1);

				int first_idx = convertCardIndex(r, s);

				master_odds_index.get(first_idx).add(row);

			}
		}

	}

	// Second attempt to make removeHand more efficient.
	public float[] removehand2(PLBadugiHand hand) {
		// mark hand to 2 if it finds hand
//		boolean foundMatch = false;
//		int counter1 = 0;
//		int counter_marked = 0;
		int currentHandScore = 0;
		int holdingHandIndex = 0;

		int r1 = hand.getAllCards().get(0).getRank();
		int s1 = hand.getAllCards().get(0).getSuit();
		int r2 = hand.getAllCards().get(1).getRank();
		int s2 = hand.getAllCards().get(1).getSuit();
		int r3 = hand.getAllCards().get(2).getRank();
		int s3 = hand.getAllCards().get(2).getSuit();
		int r4 = hand.getAllCards().get(3).getRank();
		int s4 = hand.getAllCards().get(3).getSuit();
		int c1 = convertCardIndex(r1, s1);
		int c2 = convertCardIndex(r2, s2);
		int c3 = convertCardIndex(r3, s3);
		int c4 = convertCardIndex(r4, s4);

		List<Integer> combined = new ArrayList<Integer>();

		combined.addAll(master_odds_index.get(c1));
		combined.addAll(master_odds_index.get(c2));
		combined.addAll(master_odds_index.get(c3));
		combined.addAll(master_odds_index.get(c4));
		Collections.sort(combined);

		message(out, "\t... finding match => [" + r1 + ", " + s1 + ", " + r2 + ", " + s2 + ", " + r3 + ", " + s3 + ", "
				+ r4 + ", " + s4 + "]");

		for (int row = 0; row < master_odds.size(); row++) {

			// Finding perfect match
			// [9] =0 hand DNE, =1 still a possibility, =2 hand currently hold
			if (master_odds.get(row).get(0).intValue() == r1 && master_odds.get(row).get(1).intValue() == s1
					&& master_odds.get(row).get(2).intValue() == r2 && master_odds.get(row).get(3).intValue() == s2
					&& master_odds.get(row).get(4).intValue() == r3 && master_odds.get(row).get(5).intValue() == s3
					&& master_odds.get(row).get(6).intValue() == r4 && master_odds.get(row).get(7).intValue() == s4) {

				message(out, "\t... found => [" + master_odds.get(row).toString() + "in row" + row + "]");
				master_odds.get(row).set(9, 2);
				currentHandScore = master_odds.get(row).get(8);
//				foundMatch = true;
				holdingHandIndex = row;
				break;
			}

		}

		for (int ix : combined) {
			if (ix == holdingHandIndex) {
				continue;
			}
			if (master_odds.get(ix).get(9) != 0)
				master_odds.get(ix).set(9, 0);
//			counter_marked++;
		}

		// Calculations
		// Initialize variables.
		int numGtCurrentScore = 0;
		int numEqCurrentScore = 0;
		int numLtCurrentScore = 0;
		int noOfdiscarded = 0;

		// tally only hands that are at least power of 3
		int numLtCurrentScoreIgnoring1and2 = 0;
		int numEqCurrentScoreIgnoring1and2 = 0;
		int numGtCurrentScoreIgnoring1and2 = 0;
		int numOfdiscarded1and2 = 99125;

		// [0,1],[2,3],[4,5],[6,7] cards
		// [8] absolute rank
		// [9] card active in deck
		// [10] power of card

//    	message(out,"\t... calculating odds");
		for (ArrayList<Integer> row : master_odds) {
			switch (row.get(9)) {
			case 0:
				noOfdiscarded++;
				if (row.get(10) <= 2)
					numOfdiscarded1and2--;
				break;
			case 1:
				if (currentHandScore < row.get(8)) {
					// number greater than current score (i lose)
					numGtCurrentScore++;
					if (row.get(10) > 2)
						numGtCurrentScoreIgnoring1and2++;

				} else if (currentHandScore == row.get(8)) {
					// number equal current score (we draw)
					numEqCurrentScore++;
					if (row.get(10) > 2)
						numEqCurrentScoreIgnoring1and2++;

				} else {
					// number less than current score (i win)
					numLtCurrentScore++;
					if (row.get(10) > 2)
						numLtCurrentScoreIgnoring1and2++;

				}
				break;
			case 2:
				message(out, "\t\t... found status = 2. " + row.toString(), 1);
				break;
			}
		}

		float iLose = ((float) numGtCurrentScore / (270725 - noOfdiscarded));
		float iDraw = ((float) numEqCurrentScore / (270725 - noOfdiscarded));
		float iWin = ((float) numLtCurrentScore / (270725 - noOfdiscarded));

		float iLoseRealized = ((float) numGtCurrentScoreIgnoring1and2 / (270725 - numOfdiscarded1and2 - noOfdiscarded));
		float iDrawRealized = ((float) numEqCurrentScoreIgnoring1and2 / (270725 - numOfdiscarded1and2 - noOfdiscarded));
		float iWinRealized = ((float) numLtCurrentScoreIgnoring1and2 / (270725 - numOfdiscarded1and2 - noOfdiscarded));

		if ((numGtCurrentScore + numEqCurrentScore + numLtCurrentScore + noOfdiscarded + 1) != 270725) {
			message(out, "\t\t... 270725 mismatch. Total: "
					+ (numGtCurrentScore + numEqCurrentScore + numLtCurrentScore + noOfdiscarded + 1));
		}

		float[] report = new float[8];
		report[0] = iLose;
		report[1] = iDraw;
		report[2] = iWin;
		report[3] = iLoseRealized;
		report[4] = iDrawRealized;
		report[5] = iWinRealized;
		report[7] = noOfdiscarded;

		return report;

	}

}
