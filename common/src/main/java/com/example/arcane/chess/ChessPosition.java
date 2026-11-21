package com.example.arcane.chess;

import java.util.*;

/** Server-independent chess rules and position state. Coordinates use algebraic square names (a1-h8). */
public final class ChessPosition {
	private final char[] board = new char[64];
	private Side turn = Side.WHITE;
	private String castling = "KQkq";
	private int enPassant = -1, halfmove, fullmove = 1;
	private final List<ChessMove> history = new ArrayList<>();
	private final Map<String, Integer> repetitions = new HashMap<>();

	private ChessPosition() {}
	public static ChessPosition initial() { return fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"); }
	public Side turn() { return turn; }
	public List<ChessMove> history() { return List.copyOf(history); }
	public ChessPiece pieceAt(String square) { char c = board[index(square)]; return c == 0 ? null : new ChessPiece(Character.isUpperCase(c) ? Side.WHITE : Side.BLACK, Piece.fromFen(c)); }
	char pieceAtIndex(int square) { return board[square]; }
	boolean isEnPassantCapture(ChessMove move) { return index(move.to()) == enPassant && board[index(move.from())] != 0 && Piece.fromFen(board[index(move.from())]) == Piece.PAWN; }

	ChessPosition copyForSearch() {
		ChessPosition result = copy();
		result.history.addAll(history);
		result.repetitions.putAll(repetitions);
		return result;
	}

	/** Internal search helper. Only call with a move from this position's legalMoves result. */
	void applyGeneratedMove(ChessMove move) {
		int from = index(move.from()), to = index(move.to());
		applyUnchecked(from, to, move.promotion());
		history.add(move);
		repetitions.merge(repetitionKey(), 1, Integer::sum);
	}

	public static ChessPosition fromFen(String fen) {
		String[] fields = fen.trim().split("\\s+");
		if (fields.length != 6) throw new IllegalArgumentException("FEN must have six fields");
		ChessPosition p = new ChessPosition(); String[] ranks = fields[0].split("/");
		if (ranks.length != 8) throw new IllegalArgumentException("FEN must have eight ranks");
		for (int r = 0; r < 8; r++) { int f = 0;
			for (char c : ranks[r].toCharArray()) {
				if (c >= '1' && c <= '8') { f += c - '0'; if (f > 8) throw new IllegalArgumentException("Invalid FEN rank"); }
				else { Piece piece=Piece.fromFen(c); if (f >= 8 || piece==Piece.PAWN&&(r==0||r==7)) throw new IllegalArgumentException("Invalid FEN rank"); p.board[(7-r)*8+f++] = c; }
			}
			if (f != 8) throw new IllegalArgumentException("Invalid FEN rank");
		}
		p.turn = fields[1].equals("w") ? Side.WHITE : fields[1].equals("b") ? Side.BLACK : badTurn();
		p.castling = fields[2].equals("-") ? "" : fields[2];
		if (!p.castling.matches("[KQkq]*") || p.castling.chars().distinct().count()!=p.castling.length()) throw new IllegalArgumentException("Invalid castling field");
		p.enPassant = fields[3].equals("-") ? -1 : index(fields[3]);
		p.halfmove = Integer.parseInt(fields[4]); p.fullmove = Integer.parseInt(fields[5]);
		int whiteKings=0,blackKings=0;
		for(char piece:p.board){if(piece=='K')whiteKings++;if(piece=='k')blackKings++;}
		if (p.halfmove < 0 || p.fullmove < 1 || whiteKings!=1 || blackKings!=1) throw new IllegalArgumentException("Invalid chess position");
		if (p.castling.contains("K") && (p.board[4]!='K'||p.board[7]!='R') || p.castling.contains("Q") && (p.board[4]!='K'||p.board[0]!='R')
				|| p.castling.contains("k") && (p.board[60]!='k'||p.board[63]!='r') || p.castling.contains("q") && (p.board[60]!='k'||p.board[56]!='r')) throw new IllegalArgumentException("Castling right has no king or rook");
		if(p.enPassant>=0){
			int requiredRank=p.turn==Side.WHITE?5:2;
			if(p.enPassant/8!=requiredRank)throw new IllegalArgumentException("Invalid en-passant field");
			int capturedPawn=p.enPassant+(p.turn==Side.WHITE?-8:8);
			char expectedPawn=p.turn==Side.WHITE?'p':'P';
			if(p.board[p.enPassant]!=0||p.board[capturedPawn]!=expectedPawn)throw new IllegalArgumentException("Invalid en-passant field");
		}
		p.repetitions.put(p.repetitionKey(), 1); return p;
	}
	private static Side badTurn() { throw new IllegalArgumentException("Invalid FEN turn"); }
	public String toFen() {
		StringBuilder out = new StringBuilder();
		for (int r = 7; r >= 0; r--) { int empty = 0;
			for (int f = 0; f < 8; f++) { char c=board[r*8+f]; if(c==0) empty++; else { if(empty>0){out.append(empty);empty=0;} out.append(c); } }
			if(empty>0)out.append(empty); if(r>0)out.append('/');
		}
		return out + " " + (turn==Side.WHITE?"w":"b") + " " + (castling.isEmpty()?"-":castling) + " " + (enPassant<0?"-":square(enPassant)) + " " + halfmove + " " + fullmove;
	}

	public List<ChessMove> legalMoves() {
		List<ChessMove> result = new ArrayList<>();
		for(int from=0;from<64;from++) if(owned(board[from],turn)) for(int to=0;to<64;to++) {
			Piece piece=Piece.fromFen(board[from]);
			if(piece==Piece.PAWN && (to/8==0 || to/8==7)) {
				for(Piece promotion:List.of(Piece.QUEEN,Piece.ROOK,Piece.BISHOP,Piece.KNIGHT)) addIfLegal(result,from,to,promotion);
			} else addIfLegal(result,from,to,null);
		}
		return List.copyOf(result);
	}
	private void addIfLegal(List<ChessMove> out,int from,int to,Piece promotion) {
		if(!pseudoLegal(from,to,promotion))return;
		ChessPosition copy=copy(); copy.applyUnchecked(from,to,promotion);
		if(!copy.inCheck(turn))out.add(new ChessMove(square(from),square(to),promotion));
	}
	public boolean play(String from,String to) {
		int a=index(from),b=index(to);
		Piece promotion=board[a]!=0&&Piece.fromFen(board[a])==Piece.PAWN&&(b/8==0||b/8==7)?Piece.QUEEN:null;
		return play(from,to,promotion);
	}
	public boolean play(String from,String to,Piece promotion) {
		int a=index(from),b=index(to); Piece actual=Piece.fromFen(board[a]);
		if(actual==Piece.PAWN && (b/8==0||b/8==7) && promotion==null)return false;
		ChessMove candidate=new ChessMove(square(a),square(b),promotion);
		if(!legalMoves().contains(candidate))return false;
		applyUnchecked(a,b,promotion); history.add(candidate); repetitions.merge(repetitionKey(),1,Integer::sum); return true;
	}
	private boolean pseudoLegal(int a,int b,Piece promo) {
		char c=board[a]; if(c==0||a==b||owned(board[b],turn)||board[b]!=0&&Piece.fromFen(board[b])==Piece.KING)return false;
		Piece p=Piece.fromFen(c); int df=b%8-a%8, dr=b/8-a/8, direction=turn==Side.WHITE?1:-1;
		if(p==Piece.PAWN) {
			if((b/8==0||b/8==7) != (promo!=null))return false;
			if(promo!=null && promo!=Piece.QUEEN&&promo!=Piece.ROOK&&promo!=Piece.BISHOP&&promo!=Piece.KNIGHT)return false;
			if(df==0 && board[b]==0 && dr==direction)return true;
			if(df==0 && board[b]==0 && dr==2*direction && a/8==(turn==Side.WHITE?1:6)&&board[a+direction*8]==0)return true;
			if(Math.abs(df)==1&&dr==direction&&(owned(board[b],turn.opposite())||b==enPassant))return true;
			return false;
		}
		if(p==Piece.KNIGHT)return Math.abs(df)*Math.abs(dr)==2;
		if(p==Piece.KING && Math.max(Math.abs(df),Math.abs(dr))==1)return true;
		if(p==Piece.KING && dr==0&&Math.abs(df)==2)return canCastle(a,b);
		if(p==Piece.KING)return false;
		boolean diagonal=Math.abs(df)==Math.abs(dr), straight=(df==0||dr==0);
		if(p==Piece.BISHOP&&!diagonal || p==Piece.ROOK&&!straight || p==Piece.QUEEN&&!diagonal&&!straight)return false;
		int sf=Integer.signum(df),sr=Integer.signum(dr),f=a%8+sf,r=a/8+sr;
		while(f!=b%8||r!=b/8){if(board[r*8+f]!=0)return false;f+=sf;r+=sr;} return true;
	}
	private boolean canCastle(int from,int to) {
		int rank=turn==Side.WHITE?0:7; if(from!=rank*8+4||inCheck(turn))return false;
		boolean kingSide=to%8==6; String right=turn==Side.WHITE?(kingSide?"K":"Q"):(kingSide?"k":"q");
		if(!castling.contains(right))return false;
		int rook=rank*8+(kingSide?7:0); if(!owned(board[rook],turn)||Piece.fromFen(board[rook])!=Piece.ROOK)return false;
		int start=kingSide?5:1,end=kingSide?6:3;
		for(int f=start;f<=end;f++)if(board[rank*8+f]!=0)return false;
		for(int f=kingSide?5:3;kingSide?f<=6:f>=2;f+=kingSide?1:-1){ChessPosition test=copy();test.board[from]=0;test.board[rank*8+f]=turn==Side.WHITE?'K':'k';if(test.attacked(rank*8+f,turn.opposite()))return false;}
		return true;
	}
	private void applyUnchecked(int a,int b,Piece promotion) {
		char moving=board[a],captured=board[b]; Piece type=Piece.fromFen(moving); int oldEp=enPassant; enPassant=-1;
		if(type==Piece.PAWN&&b==oldEp&&captured==0&&a%8!=b%8){int capture=b+(turn==Side.WHITE?-8:8);captured=board[capture];board[capture]=0;}
		board[a]=0; board[b]=promotion==null?moving:(turn==Side.WHITE?Character.toUpperCase(promotion.fen()):promotion.fen());
		if(type==Piece.PAWN&&Math.abs(b-a)==16)enPassant=(a+b)/2;
		if(type==Piece.KING){castling=castling.replace(turn==Side.WHITE?"K":"k","").replace(turn==Side.WHITE?"Q":"q","");if(Math.abs(b-a)==2){int rank=a/8,rookFrom=rank*8+(b>a?7:0),rookTo=rank*8+(b>a?5:3);board[rookTo]=board[rookFrom];board[rookFrom]=0;}}
		if(type==Piece.ROOK)clearRookRight(a); if(captured!=0&&Piece.fromFen(captured)==Piece.ROOK)clearRookRight(b);
		halfmove=type==Piece.PAWN||captured!=0?0:halfmove+1;if(turn==Side.BLACK)fullmove++;turn=turn.opposite();
	}
	private void clearRookRight(int square) { if(square==0)castling=castling.replace("Q","");if(square==7)castling=castling.replace("K","");if(square==56)castling=castling.replace("q","");if(square==63)castling=castling.replace("k",""); }
	public boolean inCheck(Side side){int king=kingSquare(side);return king<0||attacked(king,side.opposite());}
	private int kingSquare(Side side){char k=side==Side.WHITE?'K':'k';for(int i=0;i<64;i++)if(board[i]==k)return i;return -1;}
	private boolean attacked(int target,Side by){for(int a=0;a<64;a++)if(a!=target&&owned(board[a],by)){
		Piece p=Piece.fromFen(board[a]);int df=target%8-a%8,dr=target/8-a/8;
		if(p==Piece.PAWN){if(Math.abs(df)==1&&dr==(by==Side.WHITE?1:-1))return true;continue;}
		if(p==Piece.KNIGHT){if(Math.abs(df)*Math.abs(dr)==2)return true;continue;}
		if(p==Piece.KING){if(Math.max(Math.abs(df),Math.abs(dr))==1)return true;continue;}
		boolean d=Math.abs(df)==Math.abs(dr),s=df==0||dr==0;if((p==Piece.BISHOP&&!d)||(p==Piece.ROOK&&!s)||(p==Piece.QUEEN&&!d&&!s))continue;
		if(p==Piece.BISHOP||p==Piece.ROOK||p==Piece.QUEEN){int f=a%8+Integer.signum(df),r=a/8+Integer.signum(dr);boolean clear=true;while(f!=target%8||r!=target/8){if(board[r*8+f]!=0){clear=false;break;}f+=Integer.signum(df);r+=Integer.signum(dr);}if(clear)return true;}
	}return false;}
	public boolean isCheckmate(){return inCheck(turn)&&legalMoves().isEmpty();}
	public boolean isStalemate(){return !inCheck(turn)&&legalMoves().isEmpty();}
	public boolean isFiftyMoveDraw(){return halfmove>=100;}
	public boolean isThreefoldRepetition(){return repetitions.getOrDefault(repetitionKey(),0)>=3;}
	public boolean isInsufficientMaterial(){
		int minors=0,bishopComplex=-1,whiteKnights=0,blackKnights=0;boolean knight=false;
		for(int square=0;square<64;square++)if(board[square]!=0&&Character.toLowerCase(board[square])!='k'){
			Piece piece=Piece.fromFen(board[square]);
			if(piece==Piece.PAWN||piece==Piece.ROOK||piece==Piece.QUEEN)return false;
			minors++;
			if(piece==Piece.KNIGHT){knight=true;if(Character.isUpperCase(board[square]))whiteKnights++;else blackKnights++;}
			if(piece==Piece.BISHOP){int complex=((square%8)+(square/8))&1;if(bishopComplex!=-1&&bishopComplex!=complex)return false;bishopComplex=complex;}
		}
		if(minors==0)return true;
		if(knight)return minors==1||minors==2&&(whiteKnights==2||blackKnights==2);
		return true;
	}
	public boolean isDraw(){return isStalemate()||isFiftyMoveDraw()||isThreefoldRepetition()||isInsufficientMaterial();}
	private String repetitionKey(){String fen=toFen();String[] f=fen.split(" ");return f[0]+" "+f[1]+" "+f[2]+" "+effectiveEnPassant();}
	private String effectiveEnPassant(){
		if(enPassant<0)return "-";
		int direction=turn==Side.WHITE?1:-1;
		int sourceRank=enPassant/8-direction;
		for(int file:new int[]{enPassant%8-1,enPassant%8+1})if(file>=0&&file<8){
			int from=sourceRank*8+file;
			if(!owned(board[from],turn)||Piece.fromFen(board[from])!=Piece.PAWN)continue;
			ChessPosition copy=copy();copy.applyUnchecked(from,enPassant,null);
			if(!copy.inCheck(turn))return square(enPassant);
		}
		return "-";
	}
	private ChessPosition copy(){ChessPosition p=new ChessPosition();System.arraycopy(board,0,p.board,0,64);p.turn=turn;p.castling=castling;p.enPassant=enPassant;p.halfmove=halfmove;p.fullmove=fullmove;return p;}
	private static boolean owned(char c,Side s){return c!=0&&(Character.isUpperCase(c)==(s==Side.WHITE));}
	private static int index(String square){if(square==null||!square.matches("[a-h][1-8]"))throw new IllegalArgumentException("Invalid square: "+square);return square.charAt(0)-'a'+(square.charAt(1)-'1')*8;}
	private static String square(int i){return ""+(char)('a'+i%8)+(char)('1'+i/8);}
}
