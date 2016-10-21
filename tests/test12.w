write('Enter a number: ');
read(a);
write('Enter a number: ');
read(b);
write('Enter a number: ');
read(c);
write('Enter a number: ');
read(d);
if ((a <= b) & ! (b = d))
  then
  ( a := b+c-d;
    if (a <= d) & ((true & (c <= b)) & !false)
      then (b := a*a*d-c*c;
            if !(b = a) & !(c <= a)
                then (c := b-b+b*a+c;
                        if (c <= b) & !((true & (d <= a)) & !false)
                            then (d := a+d+c*d+c;
                                    if !((!false & (a = a)) & (c <= d)) & (a <= d)
                                    then (d := a-b;
                                          b := c+d;
                                          a := a+b;
                                          b := c+a+d)
                                    else (d := 2-a-b;
                                          b := c+12-d;
                                          a := a+b+b-12;
                                          b := 12+c+d)
                            )
                            else (d := c*d+b+a*b;
                                    if !(((c <= a) & (d <= b)) & true) & (a <= d)
                                    then (d := 0-b;
                                          b := c+d+a;
                                          a := a+a+a;
                                          b := c+a+d+b+b)
                                    else (d := 2-a-b+20;
                                          b := c+c+12-d;
                                          a := a+b+b-12;
                                          b := c+d+200-c*c)

                            )
                )
                else ( c := b-b+b*a+c;
                        if (c <= b) & !((true & (d <= a)) & !false)
                            then (d := a+d+c*d+c;
                                    if ((!false & (a = a)) & (c <= d)) & (a <= d)
                                    then (d := a-b;
                                          b := c+d;
                                          a := a+b;
                                          c := c+a+d)
                                    else (d := 2-a-b;
                                          b := c+12-d;
                                          a := a+b+b-12;
                                          c := 12+c+d)
                            )
                            else (d := c*d+b+a*b;
                                    if !(((c <= a) & (d <= b)) & false) & !(c <= a)
                                    then (d := c+d-b;
                                          b := c+d+a-b*b;
                                          a := a+a+b+b;
                                          c := a+a+d+b+b)
                                    else (d := 2-d-a+20;
                                          b := c+c+12- c;
                                          a := a+d+d-12;
                                          d := c+d+100-a*c)

                            )

                )
      )
      else (b := 2-c*c;
            if !(b = a) & !(c <= a)
                then (c := b-b+b*a+c;
                        if (c <= b) & !((true & (d <= a)) & !false)
                            then (d := a+d+c*d+c;
                                    if !((!false & (a = a)) & (c <= d)) & (a <= d)
                                    then (d := a-b;
                                          b := c+d;
                                          a := c+a+d)
                                    else (d := 2-a-b;
                                          a := a+b+b-12;
                                          b := 12+c+d)
                            )
                            else (d := c*d+b+a*b;
                                    if !(((c <= a) & (d <= b)) & true) & (a <= d)
                                    then (d := 0-b;
                                          b := c+d+a;
                                          b := a+a+a;
                                          b := c+a+d+b+b)
                                    else (d := 2-a-b+20;
                                          a := a+b+b-12;
                                          b := c+d+200-c*c)

                            )
                )
                else ( c := b-b+b*a+c;
                        if (c <= b) & !((true & (d <= a)) & !false)
                            then (d := a+d+c*d+c;
                                    if ((!false & (a = a)) & (c <= d)) & (a <= d)
                                    then (d := a-b;
                                          b := c+d;
                                          a := a+b;
                                          a := c+a+d)
                                    else (d := 2-a-b;
                                          b := c+12-d;
                                          a := a+b+b-12;
                                          b := 12+c+d)
                            )
                            else (d := c*d+b+a*b;
                                    if !(((c <= a) & (d <= b)) & false) & !(c <= a)
                                    then (d := c+d-b;
                                          b := c+d+a-b*b;
                                          a := a+a+b+b;
                                          d := a+a+d+b+b)
                                    else (d := 2-d-a+20;
                                          b := c+c+12- c;
                                          a := a+d+d-12;
                                          b := c+d+100-a*c)

                            )
                 )

                );
    if ((a <= d) & !false) & ((a <= d) & true)
      then a := a+2
      else b := c-2
  )
  else (d := 0-b;
      b := c+d+a;
      a := a+a+a;
      b := c+a+d+b+b);

write('a = ');
write(a);
writeln;
write('b = ');
write(b);
writeln;
write('c = ');
write(c);
writeln;write('d = ');
write(d);
writeln
