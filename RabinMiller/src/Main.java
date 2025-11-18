import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger ONE  = BigInteger.ONE;
    private static final BigInteger TWO  = BigInteger.TWO;

    public static BigInteger fastPowMod(BigInteger a, BigInteger exponent, BigInteger n, String operationName) {
        BigInteger i = exponent;
        BigInteger p = ONE;
        BigInteger a_k = a.mod(n);
        int step = 1;

        System.out.println("\n" + operationName + " числа " + a + ":");
        System.out.printf("%2s | %10s | %10s | %3s | %10s%n", "k", "a_k", "i", "s", "p");
        System.out.println("---------------------------------------------");

        while (i.compareTo(ZERO) > 0) {
            int s = i.mod(TWO).intValue(); // 0 или 1
            if (s == 1) {
                p = p.multiply(a_k).mod(n);
            }
            System.out.printf("%2d | %10s | %10s | %3d | %10s%n",
                    step, a_k.toString(), i.toString(), s, p.toString());

            a_k = a_k.multiply(a_k).mod(n);
            i = i.divide(TWO);
            step++;
        }

        System.out.println("Итоговый результат: " + p + " (" + a + "^" + exponent + " mod " + n + ")\n");
        return p;
    }

    public static class PrimeGenerator {
        private final List<Integer> smallPrimes; // малые простые для ранней отбраковки
        private final SecureRandom rnd = new SecureRandom();

        public PrimeGenerator() {
            this.smallPrimes = generateSmallPrimes(200);
        }

        private List<Integer> generateSmallPrimes(int limit) {
            boolean[] sieve = new boolean[limit + 1];
            for (int i = 2; i <= limit; i++) sieve[i] = true;

            for (int i = 2; i * i <= limit; i++) {
                if (sieve[i]) {
                    for (int j = i * i; j <= limit; j += i) {
                        sieve[j] = false;
                    }
                }
            }
            List<Integer> res = new ArrayList<>();
            for (int i = 2; i <= limit; i++) {
                if (sieve[i]) res.add(i);
            }
            return res;
        }

        private BigInteger randomBetween(BigInteger min, BigInteger max) {
            BigInteger range = max.subtract(min).add(ONE); // размер диапазона
            int bitLen = range.bitLength();
            BigInteger x;
            do {
                x = new BigInteger(bitLen, rnd);
            } while (x.compareTo(range) >= 0);
            return x.add(min);
        }

        /**
         * Тест Рабина–Миллера: true = вероятно простое, false = составное
         * @param p проверяемое число
         * @param k число раундов (свидетелей)
         * @param showSteps печатать ли подробные шаги возведения в степень
         */
        public boolean rabinMillerTest(BigInteger p, int k, boolean showSteps) {
            if (p.equals(BigInteger.valueOf(2)) || p.equals(BigInteger.valueOf(3))) return true;
            if (p.compareTo(ONE) <= 0 || p.mod(TWO).equals(ZERO)) return false;

            int b = 0;
            BigInteger m = p.subtract(ONE);
            while (m.mod(TWO).equals(ZERO)) {
                m = m.shiftRight(1); // m //= 2
                b++;
            }

            System.out.println("\n--- Запуск теста Рабина–Миллера для числа " + p + " ---");
            System.out.println("Разложение: " + p.subtract(ONE) + " = 2^" + b + " * " + m);

            int passedTests = 0;

            for (int testNum = 1; testNum <= k; testNum++) {
                BigInteger a = randomBetween(BigInteger.valueOf(2), p.subtract(BigInteger.valueOf(2)));
                System.out.println("\n=== Тест #" + testNum + ": свидетель a = " + a + " ===");

                BigInteger z;
                if (showSteps) {
                    z = fastPowMod(a, m, p, "Вычисление a^m mod p");
                } else {
                    z = a.modPow(m, p);
                    System.out.println("Вычисление z = a^m mod p = " + a + "^" + m + " mod " + p + " = " + z);
                }

                if (z.equals(ONE) || z.equals(p.subtract(ONE))) {
                    passedTests++;
                    double errorProb = Math.pow(0.25, passedTests);
                    double confidence = (1.0 - errorProb) * 100.0;

                    System.out.println("✓ Тест #" + testNum + " пройден (z = " + z + ")");
                    System.out.println("  Пройдено тестов: " + passedTests + "/" + testNum);
                    System.out.printf("  Текущая вероятность простоты: %.6f%%%n", confidence);
                    continue;
                }

                int j = 0;
                boolean testPassed = false;

                System.out.println("Начальное значение z = " + z);
                System.out.println("Цикл по j от 0 до " + (b - 1) + ":");

                while (j < b - 1) {
                    j++;

                    System.out.println("  j = " + j + ":");
                    BigInteger zPrev = z;

                    if (showSteps) {
                        z = fastPowMod(z, TWO, p, "Возведение в квадрат (шаг " + j + ")");
                        System.out.println("    z = z_prev² mod p = " + zPrev + "² mod " + p + " = " + z);
                    } else {
                        z = z.modPow(TWO, p);
                        System.out.println("    z = z_prev² mod p = " + zPrev + "² mod " + p + " = " + z);
                    }

                    if (j > 0 && z.equals(ONE)) {
                        System.out.println("✗ Тест #" + testNum + " провален (j=" + j + ", z=1)");
                        System.out.println("  Пройдено тестов до провала: " + passedTests + "/" + testNum);
                        if (passedTests > 0) {
                            double finalErrorProb = Math.pow(0.25, passedTests);
                            double finalConfidence = (1.0 - finalErrorProb) * 100.0;
                            System.out.printf("  Итоговая вероятность простоты: %.6f%%%n", finalConfidence);
                        }
                        return false;
                    }

                    if (z.equals(p.subtract(ONE))) {
                        passedTests++;
                        double errorProb = Math.pow(0.25, passedTests);
                        double confidence = (1.0 - errorProb) * 100.0;

                        System.out.println("✓ Тест #" + testNum + " пройден (j=" + j + ", z=" + p.subtract(ONE) + ")");
                        System.out.println("  Пройдено тестов: " + passedTests + "/" + testNum);
                        System.out.printf("  Текущая вероятность простоты: %.6f%%%n", confidence);
                        testPassed = true;
                        break;
                    } else {
                        System.out.println("    z = " + z + " ≠ " + p.subtract(ONE) + ", продолжаем...");
                    }
                }

                if (!testPassed && !z.equals(p.subtract(ONE))) {
                    System.out.println("✗ Тест #" + testNum + " провален (j достигло " + b + ", z=" + z + " ≠ " + p.subtract(ONE) + ")");
                    System.out.println("  Пройдено тестов до провала: " + passedTests + "/" + testNum);
                    if (passedTests > 0) {
                        double finalErrorProb = Math.pow(0.25, passedTests);
                        double finalConfidence = (1.0 - finalErrorProb) * 100.0;
                        System.out.printf("  Итоговая вероятность простоты: %.6f%%%n", finalConfidence);
                    }
                    return false;
                }
            }

            double finalErrorProb = Math.pow(0.25, passedTests);
            double finalConfidence = (1.0 - finalErrorProb) * 100.0;

            System.out.println("\n ВСЕ ТЕСТЫ ПРОЙДЕНЫ УСПЕШНО!");
            System.out.println(" Итоговые результаты:");
            System.out.println("   Пройдено тестов: " + passedTests + "/" + k);
            System.out.printf("   Вероятность простоты: %.8f%%%n", finalConfidence);

            return true;
        }

        public boolean isDivisibleBySmallPrimes(BigInteger n) {
            for (int prime : smallPrimes) {
                if (BigInteger.valueOf(prime).compareTo(n) >= 0) break;
                if (n.mod(BigInteger.valueOf(prime)).equals(ZERO)) return true;
            }
            return false;
        }

        private BigInteger ensureMSBAndLSB(BigInteger x, int bits) {
            // Установка старшего бита = 1 и младшего бита = 1
            x = x.setBit(bits - 1);
            x = x.setBit(0);
            if (x.bitLength() != bits) {
                x = BigInteger.ONE.shiftLeft(bits - 1).or(BigInteger.ONE);
            }
            return x;
        }

        private BigInteger minCandidate(int bits) {
            return BigInteger.ONE.shiftLeft(bits - 1).or(BigInteger.ONE);
        }

        // НОВАЯ версия: идём от стартового случайного кандидата шагом +2
        public BigInteger generatePrime(int bits) {
            // 1) стартовый случайный кандидат с нужными битами
            BigInteger p = new BigInteger(bits, rnd);
            p = ensureMSBAndLSB(p, bits);

            // Сохраняем старт, чтобы обнаружить полный круг
            BigInteger start = p;

            while (true) {
                // Быстрый отсев малыми простыми
                if (!isDivisibleBySmallPrimes(p)) {
                    // Рабин–Миллер
                    if (rabinMillerTest(p, 5, false)) {
                        System.out.println("\n Найдено вероятно простое число: " + p);
                        System.out.println(" Битовая длина: " + p.bitLength() + " бит\n");
                        return p;
                    }
                }

                // 2) Переходим к следующему нечётному кандидату
                p = p.add(TWO);

                // 3) Если вылезли за пределы битовой длины — начинаем с минимального кандидата
                if (p.bitLength() > bits) {
                    p = minCandidate(bits);
                }

                // 4) Если сделали полный круг и ничего не нашли — берём новый случайный старт
                if (p.equals(start)) {
                    p = ensureMSBAndLSB(new BigInteger(bits, rnd), bits);
                    start = p; // перезапускаем цикл поиска
                }
            }
        }
    }

    public static void main(String[] args) {
        PrimeGenerator generator = new PrimeGenerator();

        System.out.println("\nГенерация 21-битного простого числа...");
        BigInteger prime = generator.generatePrime(21);
    }
}
