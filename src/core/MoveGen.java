package core;

import java.util.Iterator;
import java.util.LinkedList;

public class MoveGen {

	public static LinkedList<Move> generateMoves(BitBoard board, boolean legal) {
		LinkedList<Move> moves = new LinkedList<>();
		// Chỉ tạo các bước di chuyển cho mặt tiếp theo để di chuyển
		int side = board.toMove;
		addPawnPushes(board, moves, side);
		long pawnBoard = board.getBitBoards()[side + 2];
		while (pawnBoard != 0) {
			addPawnAttacks(board, moves, BitBoard.bitScanForward(pawnBoard), side);
			pawnBoard &= pawnBoard - 1;
		}
		long knightBoard = board.getBitBoards()[side + 4];
		while (knightBoard != 0) {
			// Thêm nước đi cho mỗi quân mã
			addKnightMoves(board, moves, BitBoard.bitScanForward(knightBoard), side);
			knightBoard &= knightBoard - 1;
		}
		long rookBoard = board.getBitBoards()[side + 6];
		while (rookBoard != 0) {
			// Thêm nước đi cho từng quân xe
			addRookMoves(board, moves, BitBoard.bitScanForward(rookBoard), side);
			rookBoard &= rookBoard - 1;
		}
		long bishopBoard = board.getBitBoards()[side + 8];
		while (bishopBoard != 0) {
			// Thêm nước đi cho từng quân tượng
			addBishopMoves(board, moves, BitBoard.bitScanForward(bishopBoard), side);
			bishopBoard &= bishopBoard - 1;
		}
		long queenBoard = board.getBitBoards()[side + 10];
		while (queenBoard != 0) {
			// Thêm nước đi cho quân hậu
			addQueenMoves(board, moves, BitBoard.bitScanForward(queenBoard), side);
			queenBoard &= queenBoard - 1;
		}
		long kingBoard = board.getBitBoards()[side + 12];
		while (kingBoard != 0) {
			// Thêm nước đi cho quân vua
			addKingMoves(board, moves, BitBoard.bitScanForward(kingBoard), side);
			kingBoard &= kingBoard - 1;
		}

		Iterator<Move> iter = moves.iterator();
		while (iter.hasNext()) {
			Move move = iter.next();
			if (board.getBoardArray()[move.getFinalPos()] == CoreConstants.WHITE_KING
					|| board.getBoardArray()[move.getFinalPos()] == CoreConstants.BLACK_KING) {
				iter.remove();
			}
			board.move(move);
			boolean noWKings = BitBoard.bitScanForward(board.getBitBoards()[CoreConstants.WHITE_KING]) == -1;
			boolean noBKings = BitBoard.bitScanForward(board.getBitBoards()[CoreConstants.BLACK_KING]) == -1;
			if(noWKings | noBKings){
				System.out.println("KING OFF");
				iter.remove();
			}
			board.undo();
		}
		if (legal) {
			moves = removeCheckMoves(board, moves, side);
		}
		return moves;
	}

	private static boolean kingInKingSquare(BitBoard board, int side) {
		int myKingIndex = BitBoard.bitScanForward(board.getBitBoards()[12 + side]);
		int enemyKingIndex = BitBoard
				.bitScanForward(board.getBitBoards()[12 + ((side == 0) ? 1 : 0)]);
		if (((1L << myKingIndex) & CoreConstants.KING_TABLE[enemyKingIndex]) != 0) {
			return true;
		}
		return false;
	}

	private static LinkedList<Move> removeCheckMoves(BitBoard board, LinkedList<Move> moveList,
			int side) {

		Iterator<Move> iter = moveList.iterator();
		while (iter.hasNext()) {
			Move move = iter.next();
			int pieceSide = move.getPieceType() % 2;
			if (pieceSide == side) {
				board.move(move);
				boolean check = board.check(side);
				boolean kingInKingSquare = kingInKingSquare(board, side);
				board.undo();
				if (check | kingInKingSquare) {
					iter.remove();
				}

			}
		}
		return moveList;
	}

/*	private static LinkedList<Move> removeKingInKingSquareMoves(BitBoard board,
			LinkedList<Move> moveList, int side) {
		// Iterator has to be used to avoid concurrent modification exception
		// i.e. so that we can remove from the LinkedList as we loop through it
		//
		Iterator<Move> iter = moveList.iterator();
		while (iter.hasNext()) {
			Move move = iter.next();
			int pieceSide = move.getPieceType() % 2;
			if (pieceSide == side) {
				board.move(move);
				// If it results in check for the player's king
				// Or the king moves in the square surrounding another king
				// The move is invalid and hence removed
				// $\label{code:listRemove}$
				boolean kingInKingSquare = kingInKingSquare(board, side);
				board.undo();
				if (kingInKingSquare) {
					iter.remove();
				}

			}
		}
		return moveList;
	}*/

	private static void addMoves(int pieceType, int index, long moves, LinkedList<Move> moveList,
			boolean enPassant, boolean promotion, byte castling) {
		while (moves != 0) {
			Move move = new Move(pieceType, index, BitBoard.bitScanForward(moves));
			move.setCastling(castling);
			move.setPromotion(promotion);
			move.setEnPassant(enPassant);
			moveList.add(move);
			moves &= moves - 1;
		}
	}

	private static void addMovesWithOffset(int pieceType, long moves, LinkedList<Move> moveList,
			boolean enPassant, boolean promotion, byte castling, int offset) {
		while (moves != 0) {
			int to = BitBoard.bitScanForward(moves);
			int from = (to - offset) % 64;
			if (from < 0) {
				from = 64 + from;
			}
			Move move = new Move(pieceType, from, to);
			move.setCastling(castling);
			move.setPromotion(promotion);
			move.setEnPassant(enPassant);
			moveList.add(move);
			moves &= moves - 1;
		}
	}

	private static void addRookMoves(BitBoard board, LinkedList<Move> moveList, int index,
			int side) {
		int pieceType = (side == 0) ? CoreConstants.WHITE_ROOK : CoreConstants.BLACK_ROOK;
		long rookBlockers = (board.getBitBoards()[CoreConstants.WHITE]
				| board.getBitBoards()[CoreConstants.BLACK])
				& CoreConstants.occupancyMaskRook[index];

		int lookupIndex = (int) ((rookBlockers
				* CoreConstants.magicNumbersRook[index]) >>> CoreConstants.magicShiftRook[index]);
		long moveSquares = CoreConstants.magicMovesRook[index][lookupIndex]
				& ~board.getBitBoards()[side];
		addMoves(pieceType, index, moveSquares, moveList, false, false, CoreConstants.noCastle);
	}

	private static void addBishopMoves(BitBoard board, LinkedList<Move> moveList, int index,
			int side) {
		int pieceType = (side == 0) ? CoreConstants.WHITE_BISHOP : CoreConstants.BLACK_BISHOP;
		long bishopBlockers = (board.getBitBoards()[CoreConstants.WHITE]
				| board.getBitBoards()[CoreConstants.BLACK])
				& CoreConstants.occupancyMaskBishop[index];
		int lookupIndex = (int) ((bishopBlockers
				* CoreConstants.magicNumbersBishop[index]) >>> CoreConstants.magicShiftBishop[index]);
		long moveSquares = CoreConstants.magicMovesBishop[index][lookupIndex]
				& ~board.getBitBoards()[side];
		addMoves(pieceType, index, moveSquares, moveList, false, false, CoreConstants.noCastle);
	}

	private static void addQueenMoves(BitBoard board, LinkedList<Move> moveList, int index,
			int side) {
		int pieceType = (side == 0) ? CoreConstants.WHITE_QUEEN : CoreConstants.BLACK_QUEEN;
		long rookBlockers = (board.getBitBoards()[CoreConstants.WHITE]
				| board.getBitBoards()[CoreConstants.BLACK])
				& CoreConstants.occupancyMaskRook[index];
		int lookupIndexRook = (int) ((rookBlockers
				* CoreConstants.magicNumbersRook[index]) >>> CoreConstants.magicShiftRook[index]);
		long moveSquaresRook = CoreConstants.magicMovesRook[index][lookupIndexRook]
				& ~board.getBitBoards()[side];

		long bishopBlockers = (board.getBitBoards()[CoreConstants.WHITE]
				| board.getBitBoards()[CoreConstants.BLACK])
				& CoreConstants.occupancyMaskBishop[index];
		int lookupIndexBishop = (int) ((bishopBlockers
				* CoreConstants.magicNumbersBishop[index]) >>> CoreConstants.magicShiftBishop[index]);
		long moveSquaresBishop = CoreConstants.magicMovesBishop[index][lookupIndexBishop]
				& ~board.getBitBoards()[side];

		long queenMoves = moveSquaresRook | moveSquaresBishop;
		addMoves(pieceType, index, queenMoves, moveList, false, false, CoreConstants.noCastle);
	}

	private static void addKnightMoves(BitBoard board, LinkedList<Move> moveList, int index,
			int side) {
		int pieceType = (side == 0) ? CoreConstants.WHITE_KNIGHT : CoreConstants.BLACK_KNIGHT;
		long knightMoves = CoreConstants.KNIGHT_TABLE[index] & ~board.getBitBoards()[side];
		addMoves(pieceType, index, knightMoves, moveList, false, false, CoreConstants.noCastle);
	}

	private static void addKingMoves(BitBoard board, LinkedList<Move> moveList, int index,
			int side) {
		long moves = CoreConstants.KING_TABLE[index] & ~board.getBitBoards()[side];
		int pieceType = (side == 0) ? CoreConstants.WHITE_KING : CoreConstants.BLACK_KING;
		addMoves(pieceType, index, moves, moveList, false, false, CoreConstants.noCastle);
		if (side == CoreConstants.WHITE) {
			if ((board.getCastlingFlags()[side] & 0b10000) == 16) {
				addMoves(pieceType, index, CoreConstants.wqueenside, moveList, false, false,
						CoreConstants.wQSide);
			}
			if ((board.getCastlingFlags()[side] & 0b01000) == 8) {
				addMoves(pieceType, index, CoreConstants.wkingside, moveList, false, false,
						CoreConstants.wKSide);
			}
		} else {
			if ((board.getCastlingFlags()[side] & 0b10000) == 16) {
				addMoves(pieceType, index, CoreConstants.bqueenside, moveList, false, false,
						CoreConstants.bQSide);
			}
			if ((board.getCastlingFlags()[side] & 0b01000) == 8) {
				addMoves(pieceType, index, CoreConstants.bkingside, moveList, false, false,
						CoreConstants.bKSide);
			}
		}
	}

	private static void addPawnPushes(BitBoard board, LinkedList<Move> moveList, int side) {
		int pieceType = (side == 0) ? CoreConstants.WHITE_PAWN : CoreConstants.BLACK_PAWN;
		int[] offsets = { 8, 56 };
		long[] promotions_mask = { CoreConstants.ROW_8, CoreConstants.ROW_1 };
		long[] startWithMask = { CoreConstants.ROW_3, CoreConstants.ROW_6 };
		int offset = offsets[side];
		long pawns = board.getBitBoards()[side + CoreConstants.WHITE_PAWN];
		long emptySquares = ~(board.getBitBoards()[CoreConstants.WHITE]
				| board.getBitBoards()[CoreConstants.BLACK]);
		long pushes = (side == 0 ? (pawns << 8) : (pawns >>> 8)) & emptySquares;
		addMovesWithOffset(pieceType, pushes & ~promotions_mask[side], moveList, false, false,
				CoreConstants.noCastle, offset);
		long promotions = pushes & promotions_mask[side];
		addMovesWithOffset(pieceType, promotions, moveList, false, true, CoreConstants.noCastle,
				offset);
		pushes &= startWithMask[side];
		long doublePushes = (side == 0 ? (pushes << 8) : (pushes >>> 8)) & emptySquares;
		addMovesWithOffset(pieceType, doublePushes, moveList, false, false, CoreConstants.noCastle,
				offset + offset);
	}

	private static void addPawnAttacks(BitBoard board, LinkedList<Move> moveList, int index,
			int side) {
		int enemy = (side == 0) ? 1 : 0;
		int pawnType = (side == 0) ? CoreConstants.WHITE_PAWN : CoreConstants.BLACK_PAWN;
		long[] promotions_mask = { CoreConstants.ROW_8, CoreConstants.ROW_1 };
		long attacks = CoreConstants.PAWN_ATTACKS_TABLE[side][index] & board.getBitBoards()[enemy];
		addMoves(pawnType, index, attacks & ~promotions_mask[side], moveList, false, false,
				CoreConstants.noCastle);
		long promotions = attacks & promotions_mask[side];
		addMoves(pawnType, index, promotions, moveList, false, true, CoreConstants.noCastle);
		long enPassant = CoreConstants.PAWN_ATTACKS_TABLE[side][index]
				& board.getEpTargetSquares()[side];
		addMoves(pawnType, index, enPassant, moveList, true, false, CoreConstants.noCastle);
	}

	// http://www.rivalchess.com/magic-bitboards/
	public static void generateMoveDatabase(boolean rook) {
		long validMoves = 0;
		int variations;
		int varCount;
		int index, i, j;
		long mask;
		int magicIndex;
		int[] setBitsMask = new int[64];
		int[] setBitsIndex = new int[64];
		int bitCount = 0;
		for (index = 0; index < 64; index++) {

			mask = rook ? CoreConstants.occupancyMaskRook[index]
					: CoreConstants.occupancyMaskBishop[index];
			getIndexSetBits(setBitsMask, mask);
			bitCount = Long.bitCount(mask);
			varCount = (int) (1L << bitCount);
			for (i = 0; i < varCount; i++) {
				CoreConstants.occupancyVariation[index][i] = 0;
				getIndexSetBits(setBitsIndex, i);
				for (j = 0; setBitsIndex[j] != -1; j++) {
					CoreConstants.occupancyVariation[index][i] |= (1L << setBitsMask[setBitsIndex[j]]);
				}
			}
			variations = (int) (1L << bitCount);
			for (i = 0; i < variations; i++) {
				validMoves = 0;
				if (rook) {
					magicIndex = (int) ((CoreConstants.occupancyVariation[index][i]
							* CoreConstants.magicNumbersRook[index]) >>> CoreConstants.magicShiftRook[index]);
					for (j = index + 8; j < 64; j += 8) {
						validMoves |= (1L << j);
						if ((CoreConstants.occupancyVariation[index][i] & (1L << j)) != 0) {
							break;
						}
					}
					for (j = index - 8; j >= 0; j -= 8) {
						validMoves |= (1L << j);
						if ((CoreConstants.occupancyVariation[index][i] & (1L << j)) != 0) {
							break;
						}
					}
					for (j = index + 1; j % 8 != 0; j++) {
						validMoves |= (1L << j);
						if ((CoreConstants.occupancyVariation[index][i] & (1L << j)) != 0) {
							break;
						}
					}
					for (j = index - 1; j % 8 != 7 && j >= 0; j--) {
						validMoves |= (1L << j);
						if ((CoreConstants.occupancyVariation[index][i] & (1L << j)) != 0) {
							break;
						}
					}
					CoreConstants.magicMovesRook[index][magicIndex] = validMoves;
				} else {
					magicIndex = (int) ((CoreConstants.occupancyVariation[index][i]
							* CoreConstants.magicNumbersBishop[index]) >>> CoreConstants.magicShiftBishop[index]);
					for (j = index + 9; j % 8 != 0 && j < 64; j += 9) {
						validMoves |= (1L << j);
						if ((CoreConstants.occupancyVariation[index][i] & (1L << j)) != 0) {
							break;
						}
					}
					for (j = index - 9; j % 8 != 7 && j >= 0; j -= 9) {
						validMoves |= (1L << j);
						if ((CoreConstants.occupancyVariation[index][i] & (1L << j)) != 0) {
							break;
						}
					}
					for (j = index + 7; j % 8 != 7 && j < 64; j += 7) {
						validMoves |= (1L << j);
						if ((CoreConstants.occupancyVariation[index][i] & (1L << j)) != 0) {
							break;
						}
					}
					for (j = index - 7; j % 8 != 0 && j >= 0; j -= 7) {
						validMoves |= (1L << j);
						if ((CoreConstants.occupancyVariation[index][i] & (1L << j)) != 0) {
							break;
						}
					}
					CoreConstants.magicMovesBishop[index][magicIndex] = validMoves;

				}
			}
		}
	}


	static void getIndexSetBits(int[] setBits, long board) {
		int onBits = 0;
		while (board != 0) {
			setBits[onBits] = Long.numberOfTrailingZeros(board);
			board ^= (1L << setBits[onBits++]);
		}
		setBits[onBits] = -1;
	}

	public static void initialiseKnightLookupTable() {
		for (int square = 0; square < 64; square++) {
			long target = 1L << square;

			long NNE = (target << 17) & ~CoreConstants.FILE_A;
			long NEE = (target << 10) & ~CoreConstants.FILE_A & ~CoreConstants.FILE_B;
			long SEE = (target >>> 6) & ~CoreConstants.FILE_A & ~CoreConstants.FILE_B;
			long SSE = (target >>> 15) & ~CoreConstants.FILE_A;
			long NNW = (target << 15) & ~CoreConstants.FILE_H;
			long NWW = (target << 6) & ~CoreConstants.FILE_G & ~CoreConstants.FILE_H;
			long SWW = (target >>> 10) & ~CoreConstants.FILE_G & ~CoreConstants.FILE_H;
			long SSW = (target >>> 17) & ~CoreConstants.FILE_H;

			CoreConstants.KNIGHT_TABLE[square] = NNE | NEE | SEE | SSE | NNW | NWW | SWW | SSW;
		}
	}

	public static void initialiseKingLookupTable() {
		for (int square = 0; square < 64; square++) {
			long target = 1L << square;
			long N = (target << 8) & ~CoreConstants.ROW_1;
			long S = (target >>> 8) & ~CoreConstants.ROW_8;
			long E = (target << 1) & ~CoreConstants.FILE_A;
			long W = (target >>> 1) & ~CoreConstants.FILE_H;
			long NE = (target << 9) & ~CoreConstants.FILE_A;
			long NW = (target << 7) & ~CoreConstants.FILE_H;
			long SE = (target >>> 7) & ~CoreConstants.FILE_A;
			long SW = (target >>> 9) & ~CoreConstants.FILE_H;
			CoreConstants.KING_TABLE[square] = N | S | E | W | NE | NW | SE | SW;
		}
	}

	public static void initialisePawnLookupTable() {
		for (int side = 0; side <= 1; side++) {
			for (int index = 0; index < 64; index++) {
				long board = 1L << index;
				CoreConstants.PAWN_ATTACKS_TABLE[side][index] = getPawnEastAttacks(board, side)
						| getPawnWestAttacks(board, side);
			}
		}

	}

	private static long getPawnEastAttacks(long board, int side) {
		long result;
		if (side == 0) {
			result = ((board << 9) & ~CoreConstants.FILE_A);

		} else {
			result = ((board >>> 7) & ~CoreConstants.FILE_A);
		}
		return result;
	}

	private static long getPawnWestAttacks(long board, int side) {
		long result;
		if (side == 0) {
			result = ((board << 7) & ~CoreConstants.FILE_H);
		} else {
			result = ((board >>> 9) & ~CoreConstants.FILE_H);
		}
		return result;
	}

}
