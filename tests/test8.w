{ Pointless benchmark program, jf13282 }

{ Do some declarations }
i:=10;
x:=i;
y:=0;
z:=1;

{ Negation and while loop }
while !(i=1) do (

  { Cheeky bit of arithmetic }
  x := x - 1;
  y := y + 3;

  { Multiplication, equality and if statement }
  if( (y*3)<=9 ) then (

    { Some boolean logic, negations, equality, etc }
    if( ((1=1)&(!(2=1))) ) then (
      z := z + 8
    ) else (
      y := y + 3
    )
  ) else (
    z := z + 1
  );

  { Superfluous (from boolean logic) statements }
  if( !(!(1=2)) & !(!(!(!(i=5)))) ) then (
    i := z
  ) else (
    i := i - 1
  )
);

{ Write and writeln statements }
write(i);
writeln;
write(x);
writeln;
write(y); 
writeln;
write(z)
