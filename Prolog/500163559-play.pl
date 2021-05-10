suits([clubs, diamonds, hearts, spades]).
ranks([deuce, three, four, five, six, seven, eight, nine, ten, jack, queen, king, ace]).
suit(X) :- suits(S), member(X, S).
rank(X) :- ranks(S), member(X, S).
card((R, S)) :- rank(R), suit(S).

lower_suit(X1,X2) :-
    suits(L),
    %append(_, [X1|Tail], L),	% reverse finding matches
  	%append(_, [X2|_], Tail).    
	append(Head,[X2|_],L),
    append(_,[X1|_],Head).
	%nth0(A,L,X1),nth0(B,L,X2),A<B.
	

lower_rank(X1,X2) :-    
    ranks(L),
  	append(Head,[X2|_],L),
    append(_,[X1|_],Head).    

lower_card((R1, S1), (R2, S2)) :-
    suit(S1),suit(S2),
    lower_rank(R1,R2).
lower_card((R1, S1), (R1, S2)) :-
    rank(R1),
    lower_suit(S1,S2).

sorted_hand([_]) :- !.
sorted_hand([X1,X2|Tail]) :-  
    lower_card(X2,X1),
    append([X2],Tail,Y),
    sorted_hand(Y)
    .
sorted_hand(H, N) :-
    length(H,N), 				% checking length
	sorted_hand(H).


% REWRITING SORTED_HAND
/*
sorted_hand([_]) :- !.
sorted_hand([X1,X2|Tail]) :-  
    lower_card(X2,X1),
    append([X2],Tail,Y),
    sorted_hand(Y)
    .
sorted_hand(H, N) :-
    length(H,N), 				% checking length
	sorted_hand(H).
*/


four_of_kind([(R1,A), (R1,B), (R1,C), (R1,D), Wild]) :-
    sorted_hand([(R1,A) ,(R1,B) ,(R1,C) ,(R1,D), Wild],5).
four_of_kind([Wild,(R1,A), (R1,B), (R1,C), (R1,D)]) :-    
    sorted_hand([Wild,(R1,A), (R1,B), (R1,C), (R1,D)],5).

full_house([(R1,A), (R1,B), (R1,C), (R2,D), (R2,E)]) :-
    sorted_hand([(R1,A) ,(R1,B) ,(R1,C) ,(R2,D) ,(R2,E)],5).
full_house([(R2,D), (R2,E), (R1,A), (R1,B), (R1,C)]) :-
    sorted_hand([(R2,D), (R2,E) ,(R1,A), (R1,B), (R1,C)],5).

flush([(A,S),(B,S),(C,S),(D,S),(E,S)]):-
    ranks(L),    
	sorted_hand([(A,S),(B,S),(C,S),(D,S),(E,S)],5),
    not(append(_, [E,D,C,B,A|_], L)),						% minus consecutive rank
	not((append([E,D,C,B], Tail, L),append(_,[A],Tail))).	% minus A,5,4,3,2

straight_flush([(A,S),(B,S),(C,S),(D,S),(E,S)]):-
    ranks(L),    
	sorted_hand([(A,S),(B,S),(C,S),(D,S),(E,S)],5),
    append(_, [E,D,C,B,A|_], L).
straight_flush([(A,S),(B,S),(C,S),(D,S),(E,S)]):-
    ranks(L),    
	sorted_hand([(A,S),(B,S),(C,S),(D,S),(E,S)],5),
	append([E,D,C,B], Tail, L),append(_,[A],Tail).

straight([(A,S1),(B,S2),(C,S3),(D,S4),(E,S5)]):-
    ranks(L),
    append(_, [E,D,C,B,A|_], L),    
	sorted_hand([(A,S1),(B,S2),(C,S3),(D,S4),(E,S5)],5),
	\+ (S1 == S2, S1 == S3, S1 == S4, S1 == S5).
straight([(A,S1),(B,S2),(C,S3),(D,S4),(E,S5)]):-
    ranks(L),
    append([E,D,C,B], Tail, L),append(_,[A],Tail),        
	sorted_hand([(A,S1),(B,S2),(C,S3),(D,S4),(E,S5)],5),
	\+ (S1 == S2, S1 == S3, S1 == S4, S1 == S5).			% negate same suit finds

% same, same, same, wild1, wild2
three_of_kind([(R1,A), (R1,B), (R1,C), (R2,D), (R3,E)]):-
    sorted_hand([(R1,A) ,(R1,B) ,(R1,C), (R2,D), (R3,E)],5),
    \+ (R1 == R2),
    \+ (R1 == R3),
    \+ (R2 == R3).

% wild1, wild2, same, same, same
three_of_kind([(R2,D), (R3,E), (R1,A), (R1,B), (R1,C)]) :-
    sorted_hand([(R2,D), (R3,E), (R1,A), (R1,B), (R1,C)],5),
    \+ (R1 == R2),
    \+ (R1 == R3),
    \+ (R2 == R3).

% wild1, same, same, same, wild2
three_of_kind([(R2,D), (R1,A), (R1,B), (R1,C), (R3,E)]) :-
    sorted_hand([(R2,D), (R1,A), (R1,B), (R1,C), (R3,E)],5),
    \+ (R1 == R2),
    \+ (R1 == R3),
    \+ (R2 == R3).

two_pair([(R1,A), (R1,B), (R2,C), (R2,D), (R3,E)]):-
    sorted_hand([(R1,A) ,(R1,B) ,(R2,C), (R2,D), (R3,E)],5),
    \+ (R1 == R2),
    \+ (R1 == R3),
    \+ (R2 == R3).

two_pair([(R1,A), (R1,B), (R3,E), (R2,C), (R2,D)]):-
    sorted_hand([(R1,A), (R1,B), (R3,E), (R2,C), (R2,D)],5),
    \+ (R1 == R2),
    \+ (R1 == R3),
    \+ (R2 == R3).

two_pair([(R3,E), (R1,A), (R1,B), (R2,C), (R2,D)]):-
    sorted_hand([(R3,E), (R1,A), (R1,B), (R2,C), (R2,D)],5),
    \+ (R1 == R2),
    \+ (R1 == R3),
    \+ (R2 == R3).

one_pair([(R1,A), (R1,B), (R2,C), (R3,D), (R4,E)]):-
    sorted_hand([(R1,A) ,(R1,B) ,(R2,C), (R3,D), (R4,E)],5),
    \+ (R1 == R2),
    \+ (R1 == R3),
    \+ (R1 == R4),
    \+ (R2 == R3),
    \+ (R3 == R4),
    \+ (R2 == R4).

one_pair([(R2,C), (R1,A), (R1,B), (R3,D), (R4,E)]):-
    sorted_hand([(R2,C), (R1,A), (R1,B), (R3,D), (R4,E)],5),
    \+ (R1 == R2),
    \+ (R1 == R3),
    \+ (R1 == R4),
    \+ (R2 == R3),
    \+ (R3 == R4),
    \+ (R2 == R4).

one_pair([(R2,C), (R3,D), (R1,A), (R1,B), (R4,E)]):-
    sorted_hand([(R2,C), (R3,D), (R1,A), (R1,B), (R4,E)],5),
    \+ (R1 == R2),
    \+ (R1 == R3),
    \+ (R1 == R4),
    \+ (R2 == R3),
    \+ (R3 == R4),
    \+ (R2 == R4).

one_pair([(R2,C), (R3,D), (R4,E), (R1,A), (R1,B)]):-
    sorted_hand([(R2,C), (R3,D), (R4,E), (R1,A), (R1,B)],5),
    \+ (R1 == R2),
    \+ (R1 == R3),
    \+ (R1 == R4),
    \+ (R2 == R3),
    \+ (R3 == R4),
    \+ (R2 == R4).

/* Query: query to test
   X: Term to solve in query
   Inf: number of inferences needed to execute the query
   Res: the variable representing the list of all solutions
   Test: query that must be true for Res for test to pass
*/
test(Query, X, Inf, Res, Test) :-
    statistics(inferences, I1),
    call(findall(X, Query, Res)),
    statistics(inferences, I2),
    Inf is I2 - I1,
    call(Test) -> 
    	(write('success '), write(Inf), nl, !) ;
    	(write('failure '), write(Res), nl, fail).
test(_, _, 0, _, _).


/*
TEST 1
*/
test_all(Inf) :-
    write('1. Card comparisons: '),
    /* How many cards are between eight of spades and jack of diamonds? */
	test((lower_card(C, (jack, diamonds)), lower_card((eight, spades), C)),
         C, Inf1, R1, length(R1, 9)),
    
    write('2. Sorting your hand: '),
    /* How many sorted five-card poker hands have the queen of hearts and then
     * the six of diamonds specifically as the fourth card? */ 
    test((H1 = [_, _, _, (six, diamonds), _], sorted_hand(H1, 5), member((queen, hearts), H1)),
         H1, Inf2, R2, length(R2, 8976)),
    
    write('3. Four of a kind: '),
    /* How many four of kind hands contain the jack of diamonds? */
    test((four_of_kind(H2), member((jack, diamonds), H2)), H2, Inf3, R3, length(R3, 60)),
    
    write('4. Full house: '),
    /* How many full houses have a diamond as second card, and jack of spades as third? */
    test((H3=[_, (_, diamonds), (jack, spades), _, _], full_house(H3)), H3, Inf4, R4,
         length(R4, 18)), 
    
    write('5. Flush: '),
    /* How many flushes have a ten as second card, and a six as a third card? */
    test((H4=[_, (ten, _), (six, _), _, _], flush(H4)), H4, Inf5, R5, length(R5, 96)),
    
    write('6. Straight: '),
    /* How many straights start and end with a diamond? */
    test((H5=[(_,diamonds),_,_,_,(_, diamonds)], straight(H5)), H5, Inf6, R6, length(R6, 630)),
    
    write('7. Straight flush: '),
    /* How many straight flushes do not contain an eight? */
    test((straight_flush(H6), not(member((eight, _), H6))), H6, Inf7, R7, length(R7, 20)),
    
    write('8. Three of a kind: '),
    /* How many three of a kind hands do not contain any spades? */
    test((three_of_kind(H7), not(member((_, spades), H7))), H7, Inf8, R8, length(R8, 7722)),
    
    write('9. One pair: '),
    /* How many hands that have one pair have the suit pattern HSHSH? */
    test((H8=[(_,hearts), (_,spades), (_,hearts), (_, spades), (_, hearts)], one_pair(H8)),
         H8, Inf9, R9, length(R9, 1430)),
    
    write('10. Two pair: '),
    /* How many sorted two pair hands have the suit pattern C*C*H ? */
    test((H9 = [(_, clubs),(_, _),(_, clubs),(_, _),(_, hearts)], two_pair(H9)), H9, Inf10,
         R10, length(R10, 858)),
    
    /* Total inferences */
    Inf is Inf1 + Inf2 + Inf3 + Inf4 + Inf5 + Inf6 + Inf7 + Inf8 + Inf9 + Inf10.


test_all2(Inf) :-
    write('1. Card comparisons: '),
    /* How many sorted hands contain at least one card from each suit? */
	test((sorted_hand(H1,5), memberchk((_,spades),H1), memberchk((_,hearts),H1), memberchk((_,diamonds),H1), (memberchk((_,clubs),H1))),
         H1, Inf1, R1, length(R1, 685464)),
    
    write('2. No pairs: '),
    /* How many hands made of cards lower than nine don't contain any pairs? */
    test((lower_rank(R, nine),H11=[(R,_)|_],sorted_hand(H11, 5), not(one_pair(H11) ; two_pair(H11) ; three_of_kind(H11) ; four_of_kind(H11) ; full_house(H11))),
         H11, Inf2, R2, length(R2, 21504)),
    
    write('3. Sorted hand: '),
    /* No sorted hand contains its first card later again. */
    test((sorted_hand([H2|T],5), member(H2, T)), H2, Inf3, R3, length(R3, 0)),
    
    write('4. Full house: '),
    /* How many full houses have seven of hearts as the middle card? */
    test((H3=[_, _, (seven, hearts), _, _], full_house(H3)), H3, Inf4, R4,
         length(R4, 42)), 
    
    write('5. Flush: '),
    /* How many inferences are needed to find out that a flush can't start and end with two different suits ? */
    test((HH = [(_,spades),_,_,_,(_,clubs)], flush(HH)),HH,Inf5,R5,length(R5, 0)),
    
    write('6. Straight: '),
    /* How many straights start with jack of spades? */
    test((H5=[(jack,spades),_,_,_,_], straight(H5)), H5, Inf6, R6, length(R6, 255)),
    
    write('7. Four of a kind: '),
    /* How many four of a kinds do not contain an eight? */
    test((four_of_kind(H6), not(member((eight, _), H6))), H6, Inf7, R7, length(R7, 528)),
    
    write('8. Three of a kind: '),
    /* How many three of a kinds have some higher and some lower additional card? */
    test( (H7 = [_,(R,_),(R,_),(R,_),_], three_of_kind(H7)), H7, Inf8, R8, length(R8, 18304)),
    
    write('9. One pair: '),
    /* No one pair is also two pair. */
    test((H8=[(_,diamonds),_,_,_,_],one_pair(H8), two_pair(H8)),
         H8, Inf9, R9, length(R9, 0)),
    
    write('10. High card: '),
    /* Using only cards lower than ten, how many hands have nothing better than a high card? */
    test((lower_rank(R, ten), X = [(R,_)|_],sorted_hand(X, 5), not(one_pair(X) ; two_pair(X) ; three_of_kind(X) ;
                                                                   four_of_kind(X) ; full_house(X) ; flush(X) ; straight(X) ;
                                                                   straight_flush(X))), X, Inf10,
         R10, length(R10, 53040)),
    
    /* Total inferences */
    Inf is Inf1 + Inf2 + Inf3 + Inf4 + Inf5 + Inf6 + Inf7 + Inf8 + Inf9 + Inf10.