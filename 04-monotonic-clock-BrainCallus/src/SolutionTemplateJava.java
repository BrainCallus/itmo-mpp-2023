import org.jetbrains.annotations.NotNull;

/**
 * В теле класса решения разрешено использовать только финальные переменные типа RegularInt.
 * Нельзя volatile, нельзя другие типы, нельзя блокировки, нельзя лазить в глобальные переменные.
 *
 * @author Churakova Alexandra
 */
public class SolutionTemplateJava implements MonotonicClock {
    private final RegularInt c1 = new RegularInt(0);
    private final RegularInt c2 = new RegularInt(0);
    private final RegularInt c3 = new RegularInt(0);
    private final RegularInt c1_check = new RegularInt(0);
    private final RegularInt c2_check = new RegularInt(0);

    @Override
    public void write(@NotNull Time time) {
        c1_check.setValue(time.getD1());
        c2_check.setValue(time.getD2());
        c3.setValue(time.getD3());
        c2.setValue(time.getD2());
        c1.setValue(time.getD1());
    }

    @NotNull
    @Override
    public Time read() {
        var readed_c1 = c1.getValue();
        var readed_c2 = c2.getValue();
        var readed_c3 = c3.getValue();
        var readed_c2_check = c2_check.getValue();
        var readed_c1_check = c1_check.getValue();
        if (readed_c1_check != readed_c1) {
            return new Time(readed_c1_check, 0, 0);
        } else if (readed_c2_check != readed_c2) {
            return new Time(readed_c1_check, readed_c2_check, 0);
        } else {
            return new Time(readed_c1_check, readed_c2_check, readed_c3);
        }
    }
}
