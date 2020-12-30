package eval;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import core.BitBoard;
import core.Move;
import core.MoveGen;

public class Search {
	private Hashtable<Integer, TranspositionEntry> hashtable = new Hashtable<>();

	public Move rootNegamax(BitBoard board, int color) {
		long overallStartTime = System.currentTimeMillis();
		double maxScore = Double.NEGATIVE_INFINITY;
		double minScore = Double.POSITIVE_INFINITY;
		Move optimal = null;
		// Tìm tất cả các nước đi có thể
		List<Move> moves = MoveGen.generateMoves(board, true);
		int noOfMoves = moves.size();
		double timePerMove = EvalConstants.THINKING_TIME / noOfMoves;
		for (Move move : moves) {
			long startTime = System.currentTimeMillis();
			double firstGuess = 0;
			for (int depth = 0; depth <= EvalConstants.MAX_DEPTH; depth++) {
				board.move(move);
				firstGuess = mtdf(board, firstGuess, depth, color);
				board.undo();
				if (System.currentTimeMillis() - startTime >= timePerMove
						&& depth >= EvalConstants.MIN_DEPTH) {
					break;
				}

			}
			if (color == EvalConstants.WHITE) {
				if (firstGuess > maxScore) {
					maxScore = firstGuess;
					optimal = move;
				}
			} else {
				if (firstGuess < minScore) {
					minScore = firstGuess;
					optimal = move;
				}
			}
		}
		System.out.println("TIME TO MAKE MOVE: " + (System.currentTimeMillis() - overallStartTime));
		return optimal;
	}

	private double mtdf(BitBoard board, double firstGuess, int depth, int color) {
		double g = firstGuess;
		double upperBound = Double.POSITIVE_INFINITY;
		double lowerBound = Double.NEGATIVE_INFINITY;
		while (lowerBound < upperBound) {
			double beta = Math.max(g, lowerBound + 1);
			g = negamax(beta - 1, beta, board, depth, color);
			if (g < beta) {
				upperBound = g;
			} else {
				lowerBound = g;
			}
		}
		return g;
	}

	private double negamax(double alpha, double beta, BitBoard board, int depth, int colorFactor) {
		double alphaOrig = alpha;
		TranspositionEntry tEntry = new TranspositionEntry();
		tEntry = hashtable.get(board.hash());
		if (tEntry != null) {
			if (tEntry.getDepth() >= depth) {
				if (tEntry.getFlag() == TranspositionFlag.EXACT) {
					return tEntry.getScore();
				} else if (tEntry.getFlag() == TranspositionFlag.LOWERBOUND) {
					alpha = Math.max(alpha, tEntry.getScore());
				} else if (tEntry.getFlag() == TranspositionFlag.UPPERBOUND) {
					beta = Math.min(beta, tEntry.getScore());
				}
			}
		}
		if (depth == 0) {
			return colorFactor * Evaluation.evaluate(board, colorFactor);
		}
		double bestValue = Double.NEGATIVE_INFINITY;
		List<Move> moves = mergeSort(board, MoveGen.generateMoves(board, false), colorFactor);
		//List<Move> moves = MoveGen.generateMoves(board, false);
		for (Move move : moves) {
			board.move(move);
			double v = -negamax(-beta, -alpha, board, depth - 1, -1 * colorFactor);
			board.undo();
			bestValue = Math.max(bestValue, v);
			alpha = Math.max(alpha, v);
			if (alpha >= beta) {
				break;
			}
		}
		TranspositionEntry tEntryFinal = new TranspositionEntry();
		tEntryFinal.setScore(bestValue);
		if (bestValue <= alphaOrig) {
			tEntryFinal.setFlag(TranspositionFlag.UPPERBOUND);
		} else if (bestValue >= beta) {
			tEntryFinal.setFlag(TranspositionFlag.LOWERBOUND);
		} else {
			tEntryFinal.setFlag(TranspositionFlag.EXACT);
		}
		tEntryFinal.setDepth(depth);
		hashtable.put(board.hash(), tEntryFinal);

		return bestValue;
	}


	public List<Move> mergeSort(BitBoard board, List<Move> moves, int colorFactor) {
		int size = moves.size();
		if (size <= 1) {
			return moves;
		}

		int middleIndex = size / 2;

		List<Move> leftList = moves.subList(0, middleIndex);
		List<Move> rightList = moves.subList(middleIndex, size);

		rightList = mergeSort(board, rightList, colorFactor);
		leftList = mergeSort(board, leftList, colorFactor);

		List<Move> result = merge(board, leftList, rightList, colorFactor);
		return result;
	}

	// Thuật toán gộp hai danh sách với nhau thành một danh sách
	public List<Move> merge(BitBoard board, List<Move> left, List<Move> right, int colorFactor) {
		List<Move> result = new LinkedList<>();
		// Tạo các trình vòng lặp cho mỗi nửa danh sách
		Iterator<Move> leftIter = left.iterator();
		Iterator<Move> rightIter = right.iterator();
		Move leftMove = leftIter.next();
		board.move(leftMove);
		double leftEval = Evaluation.fastEval(board, colorFactor);
		board.undo();
		Move rightMove = rightIter.next();
		board.move(rightMove);
		double rightEval = Evaluation.fastEval(board, colorFactor);
		board.undo();
		// Các vòng lặp while bị phá vỡ
		while (true) {
			if (colorFactor == 1) {
				if (leftEval <= rightEval) {
					result.add(leftMove);
					if (leftIter.hasNext()) {
						leftMove = leftIter.next();
						board.move(leftMove);
						leftEval = Evaluation.fastEval(board, colorFactor);
						board.undo();
					} else {
						result.add(rightMove);
						while (rightIter.hasNext()) {
							result.add(rightIter.next());
						}
						break;
					}
				} else {
					result.add(rightMove);
					if (rightIter.hasNext()) {
						rightMove = rightIter.next();
						board.move(rightMove);
						rightEval = Evaluation.fastEval(board, colorFactor);
						board.undo();
					} else {
						result.add(leftMove);
						while (leftIter.hasNext()) {
							result.add(leftIter.next());
						}
						break;
					}
				}
			} else {
				if (leftEval >= rightEval) {
					result.add(leftMove);
					if (leftIter.hasNext()) {
						leftMove = leftIter.next();
						board.move(leftMove);
						leftEval = Evaluation.fastEval(board, colorFactor);
						board.undo();
					} else {
						result.add(rightMove);
						while (rightIter.hasNext()) {
							result.add(rightIter.next());
						}
						break;
					}
				} else {
					result.add(rightMove);
					if (rightIter.hasNext()) {
						rightMove = rightIter.next();
						board.move(rightMove);
						rightEval = Evaluation.fastEval(board, colorFactor);
						board.undo();
					} else {
						result.add(leftMove);
						while (leftIter.hasNext()) {
							result.add(leftIter.next());
						}
						break;
					}
				}
			}
		}
		return result;
	}

	private enum TranspositionFlag {
		EXACT, LOWERBOUND, UPPERBOUND
	}

	private class TranspositionEntry {
		private double score;
		private TranspositionFlag flag;
		private int depth;

		public double getScore() {
			return score;
		}

		public void setScore(double score) {
			this.score = score;
		}

		public TranspositionFlag getFlag() {
			return flag;
		}

		public void setFlag(TranspositionFlag flag) {
			this.flag = flag;
		}

		public int getDepth() {
			return depth;
		}

		public void setDepth(int depth) {
			this.depth = depth;
		}
	}
}
