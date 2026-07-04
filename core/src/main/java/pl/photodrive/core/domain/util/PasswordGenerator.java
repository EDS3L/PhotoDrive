package pl.photodrive.core.domain.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generuje mocne, losowe hasło startowe spełniające reguły {@code Password} VO
 * (min. 8 znaków, wielka i mała litera, cyfra, znak specjalny). Używane przy
 * zakładaniu konta — hasło nie jest wybierane przez twórcę konta, a użytkownik
 * i tak musi je zmienić przy pierwszym logowaniu.
 */
public final class PasswordGenerator {

    // Pule bez znaków mylących wizualnie (I/l/O/0/1).
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIAL = "!@#$%^&*?-_";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;
    private static final int LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
    }

    public static String generate() {
        List<Character> chars = new ArrayList<>(LENGTH);
        // Po jednym znaku z każdej klasy — gwarantuje spełnienie reguł Password VO.
        chars.add(randomChar(UPPER));
        chars.add(randomChar(LOWER));
        chars.add(randomChar(DIGITS));
        chars.add(randomChar(SPECIAL));
        while (chars.size() < LENGTH) {
            chars.add(randomChar(ALL));
        }
        Collections.shuffle(chars, RANDOM);

        StringBuilder sb = new StringBuilder(LENGTH);
        chars.forEach(sb::append);
        return sb.toString();
    }

    private static char randomChar(String pool) {
        return pool.charAt(RANDOM.nextInt(pool.length()));
    }
}
