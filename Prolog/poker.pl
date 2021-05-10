suits([clubs, diamonds, hearts, spades]).
ranks([deuce, three, four, five, six, seven, eight, nine, ten, jack, queen, king, ace]).
suit(X) :- suits(S), member(X, S).
rank(X) :- ranks(S), member(X, S).
card((R, S)) :- rank(R), suit(S).

lower_suit(X1,X2) :-
    suits(L),    
	append(Head,[X2|_],L),
    append(_,[X1|_],Head).

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
    append(_, [E,D,C,B,A|_], L),
	sorted_hand([(A,S),(B,S),(C,S),(D,S),(E,S)],5).
straight_flush([(A,S),(B,S),(C,S),(D,S),(E,S)]):-
    ranks(L),    
    append([E,D,C,B], Tail, L),append(_,[A],Tail),
	sorted_hand([(A,S),(B,S),(C,S),(D,S),(E,S)],5).

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