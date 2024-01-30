import java.util.concurrent.locks.ReentrantLock;

/**
 * Bank implementation.
 *
 * <p>:TODO: This implementation has to be made thread-safe.
 *
 * @author : Churakova Alexandra
 */
public class BankImpl implements Bank {
    /**
     * An array of accounts by index.
     */
    private final Account[] accounts;


    /**
     * Creates new bank instance.
     *
     * @param n the number of accounts (numbered from 0 to n-1).
     */
    public BankImpl(int n) {
        accounts = new Account[n];
        for (int i = 0; i < n; i++) {
            accounts[i] = new Account();
        }
    }

    @Override
    public int getNumberOfAccounts() {
        return accounts.length;
    }

    /**
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public long getAmount(int index) {
        validateIndex(index);
        accounts[index].getLock();
        try {
            return accounts[index].getAmount();
        } finally {
            accounts[index].releaseLock();
        }
    }

    /**
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public long getTotalAmount() {
        long sum = 0;
        try {
            for (Account account : accounts) {
                account.getLock();
                sum += account.getAmount();
            }
        } finally {
            for (Account account : accounts) {
                account.releaseLock();
            }
        }
        return sum;
    }

    /**
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public long deposit(int index, long amount) {
        validateIndex(index);
        validateAmount(amount);
        accounts[index].getLock();
        try {
            accounts[index].increaseAmount(amount);
            return accounts[index].getAmount();
        } finally {
            accounts[index].releaseLock();
        }
    }

    /**
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public long withdraw(int index, long amount) {
        validateIndex(index);
        validateAmount(amount);
        accounts[index].getLock();
        try {
            accounts[index].decreaseAmount(amount);
            return accounts[index].getAmount();
        } finally {
            accounts[index].releaseLock();
        }
    }

    /**
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public void transfer(int fromIndex, int toIndex, long amount) {
        validateIndex(fromIndex);
        validateIndex(toIndex);
        if (fromIndex == toIndex)
            throw new IllegalArgumentException("fromIndex == toIndex");
        validateAmount(amount);
        if (fromIndex < toIndex) {
            accounts[fromIndex].getLock();
            accounts[toIndex].getLock();
        } else {
            accounts[toIndex].getLock();
            accounts[fromIndex].getLock();
        }

        try {
            accounts[fromIndex].decreaseAmount(amount);
            accounts[toIndex].increaseAmount(amount);
        } finally {
            accounts[fromIndex].releaseLock();
            accounts[toIndex].releaseLock();
        }
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= accounts.length) {
            throw new IllegalArgumentException("Invalid index: " + index);
        }
    }

    private void validateAmount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Invalid amount: " + amount);
        }
    }


    /**
     * Private account data structure.
     */
    static class Account {

        /**
         * Amount of funds in this account.
         */
        private long amount;
        private final ReentrantLock lock = new ReentrantLock();

        public Account() {

        }

        public long getAmount() {
            return amount;
        }

        public void increaseAmount(long delta) {
            checkOverflow(amount + delta);
            amount += delta;
        }

        public void decreaseAmount(long delta) {
            checkOverflow(delta);
            checkUnderflow(amount - delta);
            amount -= delta;
        }

        void getLock() {
            lock.lock();
        }

        void releaseLock() {
            lock.unlock();
        }

        private void checkOverflow(long argument) {
            provideIllegalStateException(MAX_AMOUNT - argument, "Overflow");
        }

        private void checkUnderflow(long argument) {
            provideIllegalStateException(argument, "Underflow");
        }

        private void provideIllegalStateException(long argument, String message) {
            if (argument < 0) {
                throw new IllegalArgumentException(message);
            }
        }

    }
}
