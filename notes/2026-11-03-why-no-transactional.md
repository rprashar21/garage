# Why no @Transactional on submitEnquiry (2026-11-03)

## What @Transactional does

A transaction bundles database work so it either all succeeds or is all undone.
`@Transactional` on a method means: open the bundle when the method starts, commit
when it ends.

Two details matter:

1. **Commit happens after the method's last line.** Spring wraps the method. Your code
   runs, the method returns, then Spring commits.
2. **JPA delays the SQL.** `save()` often only records the change in memory. The
   `INSERT` is sent at commit or flush. Our id is a UUID generated in Java, so
   Hibernate has no need to hit the database early.

## What goes wrong with @Transactional

```
method starts, Spring opens a transaction
  try {
      save(enquiry)       // Hibernate only remembers it, no SQL yet
  } catch (...) { ... }   // nothing failed, so this never runs
method returns
Spring commits           // INSERT sent now, Postgres says "duplicate key"
```

The duplicate error appears at commit, outside our method. The `try/catch` has
already finished and never sees it. The customer gets a 500.

Second problem: a database error inside a transaction marks the whole transaction
as broken. Even if the insert were forced early, the "look up the winner's row"
query in the `catch` would also fail.

## What happens without it

```
method starts, no transaction
  try {
      save(enquiry)       // save() has its own transaction:
                          //   open, INSERT, commit, all inside this call
  } catch (DataIntegrityViolationException e) {
      // duplicate error arrives HERE, the commit already happened
      findBySubmissionKey(...)   // fresh, healthy transaction
  }
```

Spring Data's `save()` is transactional itself. It commits before it returns, so a
rejected insert throws from inside our `try`. We catch it, then the lookup runs in a
new clean transaction and finds the row the other request saved.

## Comparison

| | With @Transactional | Without |
|---|---|---|
| Insert committed | After our method ends | Inside `save()` |
| Duplicate error appears | Outside our `try/catch` | Inside our `try/catch` |
| Can we recover? | No, customer gets 500 | Yes, return existing enquiry |

## Rule of thumb

To catch a database error in your own code, the commit must happen inside the `try`.
`@Transactional` on the outer method moves the commit outside it.

This works because the method does a single write. With several writes that must
succeed together, use `@Transactional` and handle the error in a separate method or
a controller-level error handler.

---

# Simple version: explained like you are 5

## The words, in kid language

| Term | Simple meaning |
|---|---|
| Database (Postgres) | The post office that keeps every letter safe for good. |
| Hibernate / `save()` | The helper who carries your letter to the post office. |
| Transaction | A bag. Everything in the bag is posted together, or none of it is. |
| Commit | Actually posting the bag. After this, it is real and permanent. |
| Rollback | Throwing the bag away. Nothing was posted. |
| Flush | The helper walks to the post office with what is in the bag. It is not final until commit. |
| Unique constraint | The post office rule: "We never keep two letters with the same stamp number." |
| Exception | The clerk shouting "No! We already have this one!" |
| `try/catch` | You standing at the counter, ready to react if the clerk shouts. |
| `@Transactional` | A teacher saying "Put everything in my bag. I will post it when the lesson is over." |

## Our story: two requests, same stamp number

The customer double-clicks Submit. Two letters with the same stamp number (the `submissionKey`) are sent. Call them Kid A and Kid B.

Both kids first ask: "Do you already have stamp number 123?" The clerk is busy and answers "No" to both. Now both kids want to post.

## Scenario 1: no teacher (what we built)

```
Kid A walks to the counter, hands over the letter, waits.
Clerk stamps it and files it. Done. (save + commit, all in one visit)

Kid B walks to the counter, hands over the letter, waits.
Clerk: "No! We already have number 123!"   <- shouted AT THE COUNTER
Kid B is still standing there (inside try) and hears it (catch).
Kid B says: "Oh, then please show me the letter that is already filed."
Clerk shows A's letter. Kid B takes the receipt home.
```

Result: the customer gets a normal reference number. No error. Kid B reacted because they were still at the counter when the shout came.

## Scenario 2: with the teacher (`@Transactional`)

```
Teacher: "Everyone, put your letters in my bag. I post it when the lesson ends."

Kid B puts the letter in the bag.            (save: nothing is posted yet)
Kid B waits at the counter... but nobody is shouting, so nothing happens.
The try/catch ends with no problem.           (catch never runs)
Kid B leaves the room.                        (method returns)

Lesson over. Teacher walks to the post office and posts the bag. (commit)
Clerk: "No! We already have number 123!"
Kid B is gone. Only the teacher hears the shout.
Nobody can say "show me the old letter". The teacher gives up.
The customer sees a big red error. (HTTP 500)
```

The problem is when the shout happens. With the teacher, it comes after the kid has left.

## Scenario 3: the second problem, the "spoiled bag"

Say the teacher did send the helper early (a flush) and the clerk shouted while the kid was still there. Rule: once the clerk shouts about anything in the bag, the teacher marks the whole bag as spoiled. A spoiled bag accepts nothing more. If Kid B then asks "show me the old letter", the answer is "this bag is spoiled, no more questions".

Without the teacher, "show me the old letter" is a new visit with a fresh clean bag, so it works.

## Scenario 4: when you DO want the teacher's bag (bank transfer)

You move 10 pounds from Account A to Account B. That is two steps: take 10 from A, add 10 to B.

```
With the bag:  take 10 from A ... add 10 to B ... post the bag. Both happened.
               If adding to B fails, throw the bag away. A still has the 10.
Without it:    take 10 from A ... add 10 to B FAILS ... the 10 is gone forever.
```

This is what `@Transactional` is for: several steps that must succeed or fail together.

## Scenario 5: why our case is different (cinema seat)

Two people click "Book seat 5" at the same moment. The cinema rule is "one seat, one booking" (the unique constraint). The second person is told "that seat is already booked". For us the nicer ending is: "Here is your existing booking," because it is the same customer and the same enquiry.

Our method has only one step (post one letter). There is nothing to undo, so the bag adds no safety. It only moves the moment of the shout to after we have left.

## The whole flow, start to finish

```
1. Customer fills the form. The page makes a random submissionKey (the stamp number).
2. Customer clicks Submit (maybe twice).
3. Controller receives the JSON and checks it with @Valid (400 if wrong).
4. Service asks: "Is there already an enquiry with this stamp number?"
     YES -> return it. Done. (the common retry)
     NO  -> go to step 5.
5. Service builds a new Enquiry (reference, trimmed text, blanks turned to null).
6. Service calls save() inside try. The letter is posted and committed right there.
     Posted fine        -> return the new reference.
     Clerk shouts       -> catch it, look up the existing enquiry, return that.
7. Controller answers 201 with the reference, status and time.
```

The only dangerous moment is steps 4 to 6, when two kids pass step 4 together. The database rule (unique stamp number) makes sure only one letter is ever filed. The `try/catch` makes sure the loser still gets a friendly answer.

## One sentence to remember

Without `@Transactional` we hear the clerk's "No!" while we are still at the counter, so we can fix it. With `@Transactional` we hear it only after we have left, so we cannot.

---

# Clarification: the error is the same, the timing is different

The duplicate-key error is identical in both versions. What changes is **when and
where** it is thrown.

Request A and Request B are separate. Each has its own transaction. They do not
share one.

## Without @Transactional

- A: `save()` runs the insert and commits inside the call. It works.
- B: `save()` runs the insert and Postgres rejects it as a duplicate. The error is
  thrown from inside `save()`, so our `catch` runs and returns A's enquiry.

## With @Transactional

- A: `save()` only records the change in memory. The method returns, then Spring
  commits and the insert runs. It works.
- B: `save()` also only records the change in memory, so no error yet. The method
  returns, then Spring commits and the insert runs. Postgres rejects it. The error
  is thrown during that commit, after our method has finished, so the `catch` was
  already skipped.

So with `@Transactional` it is not that "no commits happen". The commit is delayed
until the method ends, and a duplicate-key failure arrives there.

| | Insert and commit happen | Error reaches our `catch`? |
|---|---|---|
| Without | inside `save()`, inside the `try` | Yes |
| With | after the method returns | No |
