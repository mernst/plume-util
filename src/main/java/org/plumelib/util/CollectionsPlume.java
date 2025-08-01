// If you edit this file, you must also edit its tests.

package org.plumelib.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/** Utility functions for Collections, including Iterators. For maps, see {@link MapsP}. */
public final class CollectionsPlume {

  /** This class is a collection of methods; it does not represent anything. */
  private CollectionsPlume() {
    throw new Error("do not instantiate");
  }

  /** The system-specific line separator string. */
  private static final String lineSep = System.lineSeparator();

  // //////////////////////////////////////////////////////////////////////
  // Collections
  //

  /**
   * Adds all elements of the Iterable to the collection. This method is just like {@code
   * Collection.addAll()}, but that method takes only a Collection, not any Iterable, as its
   * arguments.
   *
   * @param <T> the type of elements
   * @param c the collection into which elements are to be inserted
   * @param elements the elements to insert into c
   * @return true if the collection changed as a result of the call
   */
  public static <T> boolean addAll(Collection<? super T> c, Iterable<? extends T> elements) {
    boolean added = false;
    for (T elt : elements) {
      if (c.add(elt)) {
        added = true;
      }
    }
    return added;
  }

  /**
   * Returns true iff the list does not contain duplicate elements, according to {@code equals()}.
   *
   * <p>The implementation uses O(n) time and O(n) space.
   *
   * @param <T> the type of the elements
   * @param a a list
   * @return true iff a does not contain duplicate elements
   */
  @SuppressWarnings({"allcheckers:purity", "lock"}) // side effect to local state (HashSet)
  @Pure
  public static <T> boolean hasDuplicates(List<T> a) {
    HashSet<T> hs = new HashSet<>();
    if (a instanceof RandomAccess) {
      for (int i = 0; i < a.size(); i++) {
        T elt = a.get(i);
        if (!hs.add(elt)) {
          return true;
        }
      }
    } else {
      for (T elt : a) {
        if (!hs.add(elt)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true iff the list does not contain duplicate elements, according to {@code equals()}.
   *
   * <p>The implementation uses O(n) time and O(n) space.
   *
   * @param <T> the type of the elements
   * @param a a list
   * @return true iff a does not contain duplicate elements
   */
  @Pure
  public static <T> boolean hasNoDuplicates(List<T> a) {
    return !hasDuplicates(a);
  }

  /**
   * Returns true iff the list does not contain duplicate elements, according to {@code equals()}.
   *
   * <p>The implementation uses O(n) time and O(n) space.
   *
   * @param <T> the type of the elements
   * @param a a list
   * @return true iff a does not contain duplicate elements
   * @deprecated use {@link #hasNoDuplicates(List)}
   */
  @Deprecated // 2023-11-30
  // @InlineMe(
  //     replacement = "CollectionsPlume.hasNoDuplicates(a)",
  //     imports = "org.plumelib.util.CollectionsPlume")
  @Pure
  public static <T> boolean noDuplicates(List<T> a) {
    return hasNoDuplicates(a);
  }

  /**
   * Returns a copy of the list (never the original list) with duplicates (according to {@code
   * equals()}) removed, but retaining the original order. The argument is not modified.
   *
   * @param <T> type of elements of the list
   * @param l a list to remove duplicates from
   * @return a copy of the list with duplicates removed
   * @deprecated use {@link withoutDuplicates} or {@link withoutDuplicatesComparable}
   */
  @Deprecated // 2021-03-28
  public static <T> List<T> removeDuplicates(List<T> l) {
    HashSet<T> hs = new LinkedHashSet<>(l);
    List<T> result = new ArrayList<>(hs);
    return result;
  }

  /**
   * Returns a copy of the list with duplicates (according to {@code equals()}) removed, but
   * retaining the original order. May return its argument if its argument has no duplicates, but is
   * not guaranteed to do so. The argument is not modified.
   *
   * <p>If the element type implements {@link Comparable}, use {@link #withoutDuplicatesSorted} or
   * {@link #withoutDuplicatesComparable}.
   *
   * @param <T> the type of elements in {@code values}
   * @param values a list of values
   * @return the values, with duplicates removed
   */
  public static <T> List<T> withoutDuplicates(List<T> values) {
    Set<T> s = ArraySet.newArraySetOrLinkedHashSet(values);
    if (values.size() == s.size()) {
      return values;
    } else {
      return new ArrayList<>(s);
    }
  }

  /**
   * Returns a list with the same contents as its argument, but sorted and without duplicates
   * (according to {@code equals()}). May return its argument if its argument is sorted and has no
   * duplicates, but is not guaranteed to do so. The argument is not modified.
   *
   * <p>This is like {@link #withoutDuplicates}, but requires the list elements to implement {@link
   * Comparable}, and thus can be more efficient.
   *
   * @see #withoutDuplicatesComparable
   * @param <T> the type of elements in {@code values}
   * @param values a list of values
   * @return the values, with duplicates removed
   */
  public static <T extends Comparable<T>> List<T> withoutDuplicatesSorted(List<T> values) {
    // This adds O(n) time cost, and has the benefit of sometimes avoiding allocating a TreeSet.
    if (isSortedNoDuplicates(values)) {
      return values;
    }

    Set<T> set = new TreeSet<>(values);
    return new ArrayList<>(set);
  }

  /**
   * Returns a list with the same contents as its argument, but without duplicates. May return its
   * argument if its argument has no duplicates, but is not guaranteed to do so. The argument is not
   * modified.
   *
   * <p>This is like {@link #withoutDuplicatesSorted}, but it is not guaranteed to return a sorted
   * list. Thus, it is occasionally more efficient.
   *
   * <p>This is like {@link #withoutDuplicates}, but requires the list elements to implement {@link
   * Comparable}, and thus can be more efficient. If a new list is returned, this does not retain
   * the original order; the result is sorted.
   *
   * @see #withoutDuplicatesSorted
   * @param <T> the type of elements in {@code values}
   * @param values a list of values
   * @return the values, with duplicates removed
   */
  public static <T extends Comparable<T>> List<T> withoutDuplicatesComparable(List<T> values) {
    // This adds O(n) time cost, and has the benefit of sometimes avoiding allocating a TreeSet.
    if (isSortedNoDuplicates(values)) {
      return values;
    }

    Set<T> set = new TreeSet<>(values);
    if (values.size() == set.size()) {
      return values;
    } else {
      return new ArrayList<>(set);
    }
  }

  /**
   * Returns the sorted version of the list. Does not alter the list. Simply calls {@code
   * Collections.sort(List<T>, Comparator<? super T>)} on a copy.
   *
   * @return a sorted version of the list
   * @param <T> type of elements of the list
   * @param l a list to sort; is not side-effected
   * @param c a sorted version of the list
   */
  // TODO: rename to "sorted()".
  public static <T> List<T> sortList(List<T> l, Comparator<@MustCallUnknown ? super T> c) {
    List<T> result = new ArrayList<>(l);
    Collections.sort(result, c);
    return result;
  }

  /**
   * Returns true if the given list is sorted.
   *
   * @param <T> the component type of the list
   * @param values a list
   * @return true if the list is sorted
   */
  public static <T extends Comparable<T>> boolean isSorted(List<T> values) {
    if (values.isEmpty() || values.size() == 1) {
      return true;
    }

    if (values instanceof RandomAccess) {
      // Per the Javadoc of RandomAccess, an indexed for loop is faster than a foreach loop.
      int size = values.size();
      for (int i = 0; i < size - 1; i++) {
        if (values.get(i).compareTo(values.get(i + 1)) > 0) {
          return false;
        }
      }
      return true;
    } else {
      Iterator<T> iter = values.iterator();
      T previous = iter.next();
      while (iter.hasNext()) {
        T current = iter.next();
        if (previous.compareTo(current) > 0) {
          return false;
        }
        previous = current;
      }
      return true;
    }
  }

  /**
   * Returns true if the given list is sorted and has no duplicates.
   *
   * @param <T> the component type of the list
   * @param values a list
   * @return true if the list is sorted and has no duplicates
   */
  public static <T extends Comparable<T>> boolean isSortedNoDuplicates(List<T> values) {
    if (values.size() < 2) {
      return true;
    }

    if (values instanceof RandomAccess) {
      // Per the Javadoc of RandomAccess, an indexed for loop is faster than a foreach loop.
      int size = values.size();
      for (int i = 0; i < size - 1; i++) {
        if (values.get(i).compareTo(values.get(i + 1)) >= 0) {
          return false;
        }
      }
      return true;
    } else {
      Iterator<T> iter = values.iterator();
      T previous = iter.next();
      while (iter.hasNext()) {
        T current = iter.next();
        if (previous.compareTo(current) >= 0) {
          return false;
        }
        previous = current;
      }
      return true;
    }
  }

  /**
   * Returns the elements (once each) that appear more than once in the given collection.
   *
   * @param <T> the type of elements
   * @param c a collection
   * @return the elements (once each) that appear more than once in the given collection
   */
  public static <T> Collection<T> duplicates(Collection<T> c) {
    Set<T> withoutDuplicates = new HashSet<>();
    Set<T> duplicates = new LinkedHashSet<>();
    for (T elt : c) {
      if (!withoutDuplicates.add(elt)) {
        duplicates.add(elt);
      }
    }
    return duplicates;
  }

  /** All calls to deepEquals that are currently underway. */
  private static HashSet<WeakIdentityPair<Object, Object>> deepEqualsUnderway =
      new HashSet<WeakIdentityPair<Object, Object>>();

  /**
   * Determines deep equality for the elements.
   *
   * <ul>
   *   <li>If both are primitive arrays, uses java.util.Arrays.equals.
   *   <li>If both are Object[], uses java.util.Arrays.deepEquals and does not recursively call this
   *       method.
   *   <li>If both are lists, uses deepEquals recursively on each element.
   *   <li>For other types, just uses equals() and does not recursively call this method.
   * </ul>
   *
   * @param o1 first value to compare
   * @param o2 second value to compare
   * @return true iff o1 and o2 are deeply equal
   */
  @SuppressWarnings({
    "allcheckers:purity",
    "lock"
  }) // side effect to static field deepEqualsUnderway
  @Pure
  public static boolean deepEquals(@Nullable Object o1, @Nullable Object o2) {
    @SuppressWarnings("interning")
    boolean sameObject = (o1 == o2);
    if (sameObject) {
      return true;
    }
    if (o1 == null || o2 == null) {
      return false;
    }

    if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
      return Arrays.equals((boolean[]) o1, (boolean[]) o2);
    }
    if (o1 instanceof byte[] && o2 instanceof byte[]) {
      return Arrays.equals((byte[]) o1, (byte[]) o2);
    }
    if (o1 instanceof char[] && o2 instanceof char[]) {
      return Arrays.equals((char[]) o1, (char[]) o2);
    }
    if (o1 instanceof double[] && o2 instanceof double[]) {
      return Arrays.equals((double[]) o1, (double[]) o2);
    }
    if (o1 instanceof float[] && o2 instanceof float[]) {
      return Arrays.equals((float[]) o1, (float[]) o2);
    }
    if (o1 instanceof int[] && o2 instanceof int[]) {
      return Arrays.equals((int[]) o1, (int[]) o2);
    }
    if (o1 instanceof long[] && o2 instanceof long[]) {
      return Arrays.equals((long[]) o1, (long[]) o2);
    }
    if (o1 instanceof short[] && o2 instanceof short[]) {
      return Arrays.equals((short[]) o1, (short[]) o2);
    }

    WeakIdentityPair<Object, Object> mypair = WeakIdentityPair.of(o1, o2);
    if (deepEqualsUnderway.contains(mypair)) {
      return true;
    }

    if (o1 instanceof Object[] && o2 instanceof Object[]) {
      return Arrays.deepEquals((Object[]) o1, (Object[]) o2);
    }

    if (o1 instanceof List<?> && o2 instanceof List<?>) {
      List<? extends @Signed Object> l1 = (List<? extends @Signed Object>) o1;
      List<? extends @Signed Object> l2 = (List<? extends @Signed Object>) o2;
      if (l1.size() != l2.size()) {
        return false;
      }
      try {
        deepEqualsUnderway.add(mypair);
        for (int i = 0; i < l1.size(); i++) {
          Object e1 = l1.get(i);
          Object e2 = l2.get(i);
          if (!deepEquals(e1, e2)) {
            return false;
          }
        }
      } finally {
        deepEqualsUnderway.remove(mypair);
      }

      return true;
    }

    return o1.equals(o2);
  }

  /**
   * Applies the function to each element of the given iterable, producing a new list of the
   * results. The point of this method is to make mapping operations more concise. You can write
   *
   * <pre>{@code   return mapList(LemmaAnnotation::get, tokens);}</pre>
   *
   * instead of
   *
   * <pre>{@code   return tokens
   *            .stream()
   *            .map(LemmaAnnotation::get)
   *            .collect(Collectors.toList());}</pre>
   *
   * Import this method with
   *
   * <pre>import static org.plumelib.util.CollectionsPlume.mapList;</pre>
   *
   * This method is just like {@link #transform}, but with the arguments in the other order.
   *
   * <p>To perform replacement in place, see {@code List.replaceAll}.
   *
   * @param <FROM> the type of elements of the given iterable
   * @param <TO> the type of elements of the result list
   * @param f a function
   * @param iterable an iterable
   * @return a list of the results of applying {@code f} to the elements of {@code iterable}
   */
  public static <
          @KeyForBottom FROM extends @Nullable @UnknownKeyFor Object,
          @KeyForBottom TO extends @Nullable @UnknownKeyFor Object>
      List<TO> mapList(Function<? super FROM, ? extends TO> f, Iterable<FROM> iterable) {
    List<TO> result;

    if (iterable instanceof RandomAccess) {
      // Per the Javadoc of RandomAccess, an indexed for loop is faster than a foreach loop.
      List<FROM> list = (List<FROM>) iterable;
      int size = list.size();
      result = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        result.add(f.apply(list.get(i)));
      }
      return result;
    }

    if (iterable instanceof Collection) {
      result = new ArrayList<>(((Collection<?>) iterable).size());
    } else {
      result = new ArrayList<>(); // no information about size is available
    }
    for (FROM elt : iterable) {
      result.add(f.apply(elt));
    }
    return result;
  }

  /**
   * Applies the function to each element of the given array, producing a list of the results.
   *
   * <p>This produces a list rather than an array because it is problematic to create an array with
   * generic compontent type.
   *
   * <p>The point of this method is to make mapping operations more concise. Import it with
   *
   * <pre>import static org.plumelib.util.CollectionsPlume.mapList;</pre>
   *
   * This method is just like {@link #transform}, but with the arguments in the other order.
   *
   * @param <FROM> the type of elements of the given array
   * @param <TO> the type of elements of the result list
   * @param f a function
   * @param a an array
   * @return a list of the results of applying {@code f} to the elements of {@code a}
   */
  public static <
          @KeyForBottom FROM extends @Nullable @UnknownKeyFor Object,
          @KeyForBottom TO extends @Nullable @UnknownKeyFor Object>
      List<TO> mapList(
          @MustCallUnknown Function<@MustCallUnknown ? super FROM, ? extends TO> f, FROM[] a) {
    int size = a.length;
    List<TO> result = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      result.add(f.apply(a[i]));
    }
    return result;
  }

  /**
   * Applies the function to each element of the given iterable, producing a new list of the
   * results. The point of this method is to make mapping operations more concise. You can write
   *
   * <pre>{@code   return transform(tokens, LemmaAnnotation::get);}</pre>
   *
   * instead of
   *
   * <pre>{@code   return tokens
   *            .stream()
   *            .map(LemmaAnnotation::get)
   *            .collect(Collectors.toList());}</pre>
   *
   * Import this method with
   *
   * <pre>import static org.plumelib.util.CollectionsPlume.transform;</pre>
   *
   * This method is just like {@link #mapList}, but with the arguments in the other order. To
   * perform replacement in place, see {@code List.replaceAll}.
   *
   * @param <FROM> the type of elements of the given collection
   * @param <TO> the type of elements of the result list
   * @param iterable an iterable
   * @param f a function
   * @return a list of the results of applying {@code f} to the elements of {@code list}
   */
  public static <
          @KeyForBottom FROM extends @Nullable @UnknownKeyFor Object,
          @KeyForBottom TO extends @Nullable @UnknownKeyFor Object>
      List<TO> transform(
          Iterable<FROM> iterable, Function<@MustCallUnknown ? super FROM, ? extends TO> f) {
    return mapList(f, iterable);
  }

  /**
   * Returns a copy of {@code orig}, where each element of the result is a clone of the
   * corresponding element of {@code orig}.
   *
   * @param <T> the type of elements of the collection
   * @param <C> the type of the collection
   * @param orig a collection
   * @return a copy of {@code orig}, as described above
   */
  @SuppressWarnings({
    "signedness", // problem with clone()
    "nullness" // generics problem
  })
  public static <T extends @Nullable Object, C extends @Nullable Collection<T>>
      @PolyNull C cloneElements(@PolyNull C orig) {
    if (orig == null) {
      return null;
    }
    C result = UtilPlume.clone(orig);
    result.clear();
    for (T elt : orig) {
      result.add(UtilPlume.clone(elt));
    }
    return result;
  }

  // A "deep copy" uses the deepCopy() method of the DeepCopyable interface.

  /**
   * Returns a copy of {@code orig}, where each element of the result is a deep copy (according to
   * the {@code DeepCopyable} interface) of the corresponding element of {@code orig}.
   *
   * @param <T> the type of elements of the collection
   * @param <C> the type of the collection
   * @param orig a collection
   * @return a copy of {@code orig}, as described above
   */
  @SuppressWarnings({"signedness", "nullness:argument"}) // problem with clone()
  public static <T extends @Nullable DeepCopyable<T>, C extends @Nullable Collection<T>>
      @PolyNull C deepCopy(@PolyNull C orig) {
    if (orig == null) {
      return null;
    }
    C result = UtilPlume.clone(orig);
    result.clear();
    for (T elt : orig) {
      result.add(DeepCopyable.deepCopyOrNull(elt));
    }
    return result;
  }

  /**
   * Returns a new list containing only the elements for which the filter returns true. To modify
   * the collection in place, use {@code Collection#removeIf} instead of this method.
   *
   * <p>Using streams gives an equivalent list but is less efficient and more verbose:
   *
   * <pre>{@code
   * coll.stream().filter(filter).collect(Collectors.toList());
   * }</pre>
   *
   * @param <T> the type of elements
   * @param coll a collection
   * @param filter a predicate
   * @return a new list with the elements for which the filter returns true
   * @deprecated use {@link #filter} instead
   */
  @Deprecated // 2023-11-30
  // @InlineMe(
  //     replacement = "CollectionsPlume.filter(coll, filter)",
  //     imports = "org.plumelib.util.CollectionsPlume")
  public static <T> List<T> listFilter(Iterable<T> coll, Predicate<? super T> filter) {
    return filter(coll, filter);
  }

  // TODO: This should return a collection of the same type as the input.  Currently it always
  // returns a list.
  /**
   * Returns a new list containing only the elements for which the filter returns true. To modify
   * the collection in place, use {@code Collection#removeIf} instead of this method.
   *
   * <p>Using streams gives an equivalent list but is less efficient and more verbose:
   *
   * <pre>{@code
   * coll.stream().filter(filter).collect(Collectors.toList());
   * }</pre>
   *
   * @param <T> the type of elements
   * @param coll a collection
   * @param filter a predicate
   * @return a new list with the elements for which the filter returns true
   */
  public static <T> List<T> filter(Iterable<T> coll, Predicate<? super T> filter) {
    List<T> result = new ArrayList<>();
    for (T elt : coll) {
      if (filter.test(elt)) {
        result.add(elt);
      }
    }
    return result;
  }

  /**
   * Returns true if any element of the collection matches the predicate.
   *
   * <p>Using streams gives an equivalent result but is less efficient:
   *
   * <pre>{@code
   * coll.stream().anyMatch(predicate);
   * }</pre>
   *
   * @param <T> the type of elements
   * @param coll a collection
   * @param predicate a non-interfering, stateless predicate
   * @return true if any element of the collection matches the predicate
   * @see #firstMatch
   */
  public static <T> boolean anyMatch(Iterable<T> coll, Predicate<? super T> predicate) {
    for (T elt : coll) {
      if (predicate.test(elt)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if all elements of the collection match the predicate.
   *
   * <p>Using streams gives an equivalent result but is less efficient:
   *
   * <pre>{@code
   * coll.stream().allMatch(predicate);
   * }</pre>
   *
   * @param <T> the type of elements
   * @param coll a collection
   * @param predicate a non-interfering, stateless predicate
   * @return true if all elements of the collection match the predicate
   */
  public static <T> boolean allMatch(Iterable<T> coll, Predicate<? super T> predicate) {
    for (T elt : coll) {
      if (!predicate.test(elt)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if no element of the collection matches the predicate.
   *
   * <p>Using streams gives an equivalent result but is less efficient:
   *
   * <pre>{@code
   * coll.stream().noneMatch(predicate);
   * }</pre>
   *
   * @param <T> the type of elements
   * @param coll a collection
   * @param predicate a non-interfering, stateless predicate
   * @return true if no element of the collection matches the predicate
   */
  public static <T> boolean noneMatch(Iterable<T> coll, Predicate<? super T> predicate) {
    for (T elt : coll) {
      if (predicate.test(elt)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the first element of the collection that matches the predicate, or null.
   *
   * <p>Using streams gives an equivalent result but is less efficient:
   *
   * <pre>{@code
   * coll.stream().filter(predicate).firstMatch().orElse(null);
   * }</pre>
   *
   * @param <T> the type of elements
   * @param coll a collection
   * @param predicate a non-interfering, stateless predicate
   * @return the first element of the collection that matches the predicate, or null
   * @see #anyMatch
   */
  public static <T> @Nullable T firstMatch(Iterable<T> coll, Predicate<? super T> predicate) {
    for (T elt : coll) {
      if (predicate.test(elt)) {
        return elt;
      }
    }
    return null;
  }

  /**
   * Returns the first index of the given value in the list, starting at the given index. Uses
   * {@code Object.equals()} for comparison.
   *
   * @param list a list
   * @param value the value to search for
   * @param start the starting index
   * @return the index of the value in the list, at or after the given index
   */
  public static int indexOf(List<?> list, Object value, int start) {
    int idx = list.subList(start, list.size()).indexOf(value);
    return idx == -1 ? -1 : idx + start;
  }

  /**
   * Represents a replacement of one range of a collection by another collection.
   *
   * @param <T> the type of collection elements
   */
  public static class Replacement<T> {
    /** The first line to replace, inclusive. */
    public final int start;

    /** The last line to replace, <em>inclusive</em>. May be equal to {@code start}-1. */
    public final int end;

    /** The new (replacement) elements. */
    final Collection<T> elements;

    /**
     * Creates a new Replacement.
     *
     * @param start the first line to replace, inclusive
     * @param end the last line to replace, exclusive
     * @param elements the new (replacement) elements
     */
    private Replacement(int start, int end, Collection<T> elements) {
      this.start = start;
      this.end = end;
      this.elements = elements;
      if (end < start - 1) {
        throw new Error("Invalid <start,end> pair: " + this);
      }
    }

    /**
     * Creates a new Replacement.
     *
     * @param <T> the type of elements of the list
     * @param start the first line to replace, inclusive
     * @param end the last line to replace, exclusive
     * @param elements the new (replacement) elements
     * @return a new Replacement
     */
    public static <T> Replacement<T> of(int start, int end, Collection<T> elements) {
      return new Replacement<T>(start, end, elements);
    }

    @Override
    public String toString(@GuardSatisfied Replacement<T> this) {
      return "Replacement{" + start + ", " + end + ", " + elements + "}";
    }
  }

  /**
   * Performs a set of replacements on the given collection, returning the transformed result (as a
   * list).
   *
   * @param <T> the type of collection elements
   * @param c a collection
   * @param replacements the replacements to perform on the collection, in order from the beginning
   *     of the collection to the end
   * @return the transformed collection, as a new list (even if no changes were made)
   */
  public static <T> List<T> replace(Iterable<T> c, Iterable<Replacement<T>> replacements) {
    List<T> result = new ArrayList<>();
    Iterator<T> cItor = c.iterator();
    int cIndex = -1; // the index into c
    Iterator<Replacement<T>> replacementItor = replacements.iterator();
    while (replacementItor.hasNext()) {
      Replacement<T> replacement = replacementItor.next();
      while (cIndex < replacement.start - 1) {
        result.add(cItor.next());
        cIndex++;
      }
      result.addAll(replacement.elements);
      while (cIndex < replacement.end) {
        cItor.next();
        cIndex++;
      }
    }
    while (cItor.hasNext()) {
      result.add(cItor.next());
    }
    return result;
  }

  /**
   * Performs a set of replacements on the given array, returning the transformed result (as a
   * list).
   *
   * @param <T> the type of collection elements
   * @param c an array
   * @param replacements the replacements to perform on the arary, in order from the beginning of
   *     the list to the end
   * @return the transformed collection, as a list
   */
  public static <T> List<T> replace(T[] c, Collection<Replacement<T>> replacements) {
    return replace(Arrays.asList(c), replacements);
  }

  /**
   * Returns true if the second list is a subsequence (not necessarily contiguous) of the first.
   *
   * @param <T> the type of elements of the list
   * @param longer a list
   * @param shorter a list
   * @return true if the second list is a subsequence (not necessarily contiguous) of the first
   */
  // TODO: This could take as input a RandomAccess.
  @SuppressWarnings("signedness")
  public static <T> boolean isSubsequenceMaybeNonContiguous(
      Iterable<T> longer, Iterable<T> shorter) {
    Iterator<T> itorLonger = longer.iterator();
    Iterator<T> itorShorter = shorter.iterator();
    outerLoop:
    while (itorShorter.hasNext()) {
      T eltShorter = itorShorter.next();
      while (itorLonger.hasNext()) {
        T eltLonger = itorLonger.next();
        if (Objects.equals(eltShorter, eltLonger)) {
          continue outerLoop;
        }
      }
      return false;
    }
    return true;
  }

  // //////////////////////////////////////////////////////////////////////
  // SortedSet
  //

  /**
   * Returns true if the two sets contain the same elements in the same order. This is faster than
   * regular {@code equals()}, for sets with the same ordering operator, especially for sets that
   * are not extremely small.
   *
   * @param <T> the type of elements in the sets
   * @param set1 the first set to compare
   * @param set2 the first set to compare
   * @return true if the two sets contain the same elements in the same order
   */
  public static <T> boolean sortedSetEquals(SortedSet<T> set1, SortedSet<T> set2) {
    @SuppressWarnings("interning:not.interned")
    boolean sameObject = set1 == set2;
    if (sameObject) {
      return true;
    }
    if (set1.size() != set2.size()) {
      return false;
    }
    Comparator<? super T> comparator1 = set1.comparator();
    Comparator<? super T> comparator2 = set2.comparator();
    if (!Objects.equals(comparator1, comparator2)) {
      // Fall back to regular `equals`.
      return set1.equals(set2);
    }
    for (Iterator<T> itor1 = set1.iterator(), itor2 = set2.iterator(); itor1.hasNext(); ) {
      if (!Objects.equals(itor1.next(), itor2.next())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if the two sets contain the same elements in the same order. This is faster than
   * regular {@code containsAll()}, for sets with the same ordering operator, especially for sets
   * that are not extremely small.
   *
   * @param <T> the type of elements in the sets
   * @param set1 the first set to compare
   * @param set2 the first set to compare
   * @return true if the first set contains all the elements of the second set
   */
  public static <T> boolean sortedSetContainsAll(SortedSet<T> set1, SortedSet<T> set2) {
    @SuppressWarnings("interning:not.interned")
    boolean sameObject = set1 == set2;
    if (sameObject) {
      return true;
    }
    if (set1.size() < set2.size()) {
      return false;
    }
    Comparator<? super T> comparator1 = set1.comparator();
    Comparator<? super T> comparator2 = set2.comparator();
    if (!Objects.equals(comparator1, comparator2)) {
      // Fall back to regular `containsAll`.
      return set1.containsAll(set2);
    }
    if (comparator1 == null) {
      outerloopNaturalOrder:
      for (Iterator<T> itor1 = set1.iterator(), itor2 = set2.iterator(); itor2.hasNext(); ) {
        T elt2 = itor2.next();
        if (elt2 == null) {
          throw new IllegalArgumentException("null element in set 2: " + set2);
        }
        while (itor1.hasNext()) {
          T elt1 = itor1.next();
          if (elt2 == null) {
            throw new IllegalArgumentException("null element in set 2: " + set2);
          }
          @SuppressWarnings({
            "unchecked", // Java warning about generic cast
            "nullness:dereference", // next() has side effects, so elt1 isn't know to be non-null
            "signedness:method.invocation" // generics problem; #979?
          })
          int comparison = ((Comparable<T>) elt1).compareTo(elt2);
          if (comparison == 0) {
            continue outerloopNaturalOrder;
          } else if (comparison < 0) {
            return false;
          }
        }
        return false;
      }
    } else {
      outerloopComparator:
      for (Iterator<T> itor1 = set1.iterator(), itor2 = set2.iterator(); itor2.hasNext(); ) {
        T elt2 = itor2.next();
        while (itor1.hasNext()) {
          T elt1 = itor1.next();
          int comparison = comparator1.compare(elt1, elt2);
          if (comparison == 0) {
            continue outerloopComparator;
          } else if (comparison < 0) {
            return false;
          }
        }
        return false;
      }
    }
    return true;
  }

  // //////////////////////////////////////////////////////////////////////
  // ArrayList
  //

  /**
   * Returns a vector containing the elements of the enumeration.
   *
   * @param <T> type of the enumeration and vector elements
   * @param e an enumeration to convert to a ArrayList
   * @return a vector containing the elements of the enumeration
   */
  @SuppressWarnings({"JdkObsolete", "NonApiType"})
  public static <T> ArrayList<T> makeArrayList(Enumeration<T> e) {
    ArrayList<T> result = new ArrayList<>();
    while (e.hasMoreElements()) {
      result.add(e.nextElement());
    }
    return result;
  }

  /**
   * Creates an immutable list containing two elements. In Java 9+, use List.of().
   *
   * @param <E> the List's element type
   * @param e1 the first element
   * @param e2 the second element
   * @return a List containing the specified elements
   */
  public static <E> List<E> listOf(E e1, E e2) {
    ArrayList<E> result = new ArrayList<>(2);
    result.add(e1);
    result.add(e2);
    return Collections.unmodifiableList(result);
  }

  /**
   * Concatenates a list and an element into a new list.
   *
   * @param <T> the type of the list elements
   * @param list the list; is not modified by this method
   * @param lastElt the new last elemeent
   * @return a new list containing the list elements and the last element, in that order
   */
  @SuppressWarnings("unchecked")
  public static <T> List<T> append(Collection<T> list, T lastElt) {
    List<T> result = new ArrayList<>(list.size() + 1);
    result.addAll(list);
    result.add(lastElt);
    return result;
  }

  /**
   * Concatenates two lists into a new list.
   *
   * @param <T> the type of the list elements
   * @param list1 the first list
   * @param list2 the second list
   * @return a new list containing the contents of the given lists, in order
   */
  @SuppressWarnings("unchecked")
  public static <T> List<T> concatenate(Collection<T> list1, Collection<T> list2) {
    List<T> result = new ArrayList<>(list1.size() + list2.size());
    result.addAll(list1);
    result.addAll(list2);
    return result;
  }

  // Rather than writing something like ArrayListToStringArray, use
  //   v.toArray(new String[0])

  // Helper method
  /**
   * Compute (n choose k), which is (n! / (k!(n-k)!)).
   *
   * @param n number of elements from which to choose
   * @param k number of elements to choose
   * @return n choose k, or Long.MAX_VALUE if the value would overflow
   */
  private static long choose(int n, int k) {
    // From https://stackoverflow.com/questions/2201113/combinatoric-n-choose-r-in-java-math
    if (n < k) {
      return 0;
    }
    if (k == 0 || k == n) {
      return 1;
    }
    long a = choose(n - 1, k - 1);
    long b = choose(n - 1, k);
    if (a < 0 || a == Long.MAX_VALUE || b < 0 || b == Long.MAX_VALUE || a + b < 0) {
      return Long.MAX_VALUE;
    } else {
      return a + b;
    }
  }

  /**
   * Returns a list of lists of each combination (with repetition, but not permutations) of the
   * specified objects starting at index {@code start} over {@code dims} dimensions, for {@code dims
   * > 0}.
   *
   * <p>For example, createCombinations(1, 0, {a, b, c}) returns a 3-element list of singleton
   * lists:
   *
   * <pre>
   *    {a}, {b}, {c}
   * </pre>
   *
   * And createCombinations(2, 0, {a, b, c}) returns a 6-element list of 2-element lists:
   *
   * <pre>
   *    {a, a}, {a, b}, {a, c}
   *    {b, b}, {b, c},
   *    {c, c}
   * </pre>
   *
   * @param <T> type of the input list elements, and type of the innermost output list elements
   * @param dims number of dimensions: that is, size of each innermost list
   * @param start initial index
   * @param objs list of elements to create combinations of
   * @return list of lists of length dims, each of which combines elements from objs
   */
  public static <T> List<List<T>> createCombinations(
      @Positive int dims, @NonNegative int start, List<T> objs) {

    if (dims < 1) {
      throw new IllegalArgumentException();
    }

    long numResults = choose(objs.size() + dims - 1, dims);
    if (numResults > 100000000) {
      throw new Error("Do you really want to create more than 100 million lists?");
    }

    List<List<T>> results = new ArrayList<List<T>>();

    for (int i = start; i < objs.size(); i++) {
      if (dims == 1) {
        List<T> simple = new ArrayList<>();
        simple.add(objs.get(i));
        results.add(simple);
      } else {
        List<List<T>> combos = createCombinations(dims - 1, i, objs);
        for (List<T> lt : combos) {
          List<T> simple = new ArrayList<>();
          simple.add(objs.get(i));
          simple.addAll(lt);
          results.add(simple);
        }
      }
    }

    return results;
  }

  /**
   * Returns a list of lists of each combination (with repetition, but not permutations) of integers
   * from start to cnt (inclusive) over arity dimensions.
   *
   * <p>For example, createCombinations(1, 0, 2) returns a 3-element list of singleton lists:
   *
   * <pre>
   *    {0}, {1}, {2}
   * </pre>
   *
   * And createCombinations(2, 10, 2) returns a 6-element list of 2-element lists:
   *
   * <pre>
   *    {10, 10}, {10, 11}, {10, 12}, {11, 11}, {11, 12}, {12, 12}
   * </pre>
   *
   * The length of the list is (cnt multichoose arity), which is ((cnt + arity - 1) choose arity).
   *
   * @param arity size of each innermost list
   * @param start initial value
   * @param cnt maximum element value
   * @return list of lists of length arity, each of which combines integers from start to cnt
   */
  @SuppressWarnings("NonApiType")
  public static ArrayList<ArrayList<Integer>> createCombinations(
      int arity, @NonNegative int start, int cnt) {

    long numResults = choose(cnt + arity - 1, arity);
    if (numResults > 100000000) {
      throw new Error("Do you really want to create more than 100 million lists?");
    }

    ArrayList<ArrayList<Integer>> results = new ArrayList<>();

    // Return a list with one zero length element if arity is zero
    if (arity == 0) {
      results.add(new ArrayList<Integer>());
      return results;
    }

    for (int i = start; i <= cnt; i++) {
      ArrayList<ArrayList<Integer>> combos = createCombinations(arity - 1, i, cnt);
      for (ArrayList<Integer> li : combos) {
        ArrayList<Integer> simple = new ArrayList<>();
        simple.add(i);
        simple.addAll(li);
        results.add(simple);
      }
    }

    return results;
  }

  // //////////////////////////////////////////////////////////////////////
  // Iterator
  //

  /**
   * Converts an Iterator to an Iterable. The resulting Iterable can be used to produce a single,
   * working Iterator (the one that was passed in). Subsequent calls to its iterator() method will
   * fail, because otherwise they would return the same Iterator instance, which may have been
   * exhausted, or otherwise be in some indeterminate state. Calling iteratorToIterable twice on the
   * same argument can have similar problems, so don't do that.
   *
   * @param source the Iterator to be converted to Iterable
   * @param <T> the element type
   * @return source, converted to Iterable
   */
  public static <T> Iterable<T> iteratorToIterable(final Iterator<T> source) {
    if (source == null) {
      throw new NullPointerException();
    }
    return new Iterable<T>() {
      /** True if this Iterable object has been used. */
      private AtomicBoolean used = new AtomicBoolean();

      @Override
      public Iterator<T> iterator() {
        if (used.getAndSet(true)) {
          throw new Error("Call iterator() just once");
        }
        return source;
      }
    };
  }

  // Making these classes into functions didn't work because I couldn't get
  // their arguments into a scope that Java was happy with.

  /**
   * Converts an Enumeration into an Iterator.
   *
   * @param <T> the type of elements of the enumeration and iterator
   */
  public static final class EnumerationIterator<T> implements Iterator<T> {
    /** The enumeration that this object wraps. */
    Enumeration<T> e;

    /**
     * Create an Iterator that yields the elements of the given Enumeration.
     *
     * @param e the Enumeration to make into an Iterator
     */
    public EnumerationIterator(Enumeration<T> e) {
      this.e = e;
    }

    @SuppressWarnings("JdkObsolete")
    @Override
    public boolean hasNext(@GuardSatisfied EnumerationIterator<T> this) {
      return e.hasMoreElements();
    }

    @SuppressWarnings("JdkObsolete")
    @Override
    public T next(@GuardSatisfied EnumerationIterator<T> this) {
      return e.nextElement();
    }

    @Override
    public void remove(@GuardSatisfied EnumerationIterator<T> this) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Converts an Iterator into an Enumeration.
   *
   * @param <T> the type of elements of the enumeration and iterator
   */
  @SuppressWarnings("JdkObsolete")
  public static final class IteratorEnumeration<T> implements Enumeration<T> {
    /** The iterator that this object wraps. */
    Iterator<T> itor;

    /**
     * Create an Enumeration that contains the elements returned by the given Iterator.
     *
     * @param itor the Iterator to make an Enumeration from
     */
    public IteratorEnumeration(Iterator<T> itor) {
      this.itor = itor;
    }

    @Override
    public boolean hasMoreElements() {
      return itor.hasNext();
    }

    @Override
    public T nextElement(@GuardSatisfied IteratorEnumeration<T> this) {
      return itor.next();
    }
  }

  /**
   * Returns an iterator that returns the elements of {@code itor} then {@code lastElement}.
   *
   * @param <T> the type of elements of the iterator
   * @param itor an Iterator
   * @param lastElement one element
   * @return an iterator that returns the elements of {@code itor} then {@code lastElement}
   */
  public static <T> Iterator<T> iteratorPlusOne(Iterator<T> itor, T lastElement) {
    return new IteratorPlusOne<>(itor, lastElement);
  }

  /**
   * An Iterator that returns first the elements of a given iterator, then one more element.
   *
   * @param <T> the type of elements of the iterator
   */
  private static final class IteratorPlusOne<T> implements Iterator<T> {
    /** The iterator that this yields first. */
    private Iterator<T> itor;

    /** The last element that this returns. */
    private T lastElement;

    /**
     * True if this iterator has not yet yielded the lastElement element, and therefore is not done.
     */
    private boolean hasPlusOne = true;

    /**
     * Create an iterator that returns the elements of {@code itor} then {@code lastElement}.
     *
     * @param itor an Iterator
     * @param lastElement one element
     */
    public IteratorPlusOne(Iterator<T> itor, T lastElement) {
      this.itor = itor;
      this.lastElement = lastElement;
    }

    @Override
    public boolean hasNext(@GuardSatisfied IteratorPlusOne<T> this) {
      return itor.hasNext() || hasPlusOne;
    }

    @Override
    public T next(@GuardSatisfied IteratorPlusOne<T> this) {
      if (itor.hasNext()) {
        return itor.next();
      } else if (hasPlusOne) {
        hasPlusOne = false;
        return lastElement;
      } else {
        throw new NoSuchElementException();
      }
    }

    @Override
    public void remove(@GuardSatisfied IteratorPlusOne<T> this) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Returns an iterator that returns the elements of {@code itor1} then those of {@code itor2}.
   *
   * @param <T> the type of elements of the iterator
   * @param itor1 an Iterator
   * @param itor2 another Iterator
   * @return an iterator that returns the elements of {@code itor1} then those of {@code itor2}
   */
  public static <T> Iterator<T> mergedIterator2(Iterator<T> itor1, Iterator<T> itor2) {
    return new MergedIterator2<>(itor1, itor2);
  }

  /**
   * An Iterator that returns first the elements returned by its first argument, then the elements
   * returned by its second argument. Like {@link MergedIterator}, but specialized for the case of
   * two arguments.
   *
   * @param <T> the type of elements of the iterator
   * @deprecated use {@link CollectionsPlume#mergedIterator2}
   */
  @Deprecated // make package-private
  public static final class MergedIterator2<T> implements Iterator<T> {
    /** The first of the two iterators that this object merges. */
    Iterator<T> itor1;

    /** The second of the two iterators that this object merges. */
    Iterator<T> itor2;

    /**
     * Create an iterator that returns the elements of {@code itor1} then those of {@code itor2}.
     *
     * @param itor1 an Iterator
     * @param itor2 another Iterator
     * @deprecated use {@link CollectionsPlume#mergedIterator2}
     */
    @Deprecated // use {@link #mergediterator2}
    public MergedIterator2(Iterator<T> itor1, Iterator<T> itor2) {
      this.itor1 = itor1;
      this.itor2 = itor2;
    }

    @Override
    public boolean hasNext(@GuardSatisfied MergedIterator2<T> this) {
      return itor1.hasNext() || itor2.hasNext();
    }

    @Override
    public T next(@GuardSatisfied MergedIterator2<T> this) {
      if (itor1.hasNext()) {
        return itor1.next();
      } else if (itor2.hasNext()) {
        return itor2.next();
      } else {
        throw new NoSuchElementException();
      }
    }

    @Override
    public void remove(@GuardSatisfied MergedIterator2<T> this) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Returns an iterator that returns the elements of the given iterators, in turn.
   *
   * @param <T> the type of elements of the iterator
   * @param itbleOfItors a collection whose elements are iterators
   * @return an iterator that returns the elements of the given iterators, in turn
   */
  public static <T> Iterator<T> mergedIterator(Iterable<Iterator<T>> itbleOfItors) {
    return new MergedIterator<>(itbleOfItors.iterator());
  }

  /**
   * Returns an iterator that returns the elements of the given iterators, in turn.
   *
   * @param <T> the type of elements of the iterator
   * @param itorOfItors an iterator whose elements are iterators
   * @return an iterator that returns the elements of the given iterators, in turn
   */
  public static <T> Iterator<T> mergedIterator(Iterator<Iterator<T>> itorOfItors) {
    return new MergedIterator<>(itorOfItors);
  }

  // This must already be implemented someplace else.  Right??
  /**
   * An Iterator that returns the elements in each of its argument Iterators, in turn. The argument
   * is an Iterator of Iterators. Like {@link MergedIterator2}, but generalized to arbitrary number
   * of iterators.
   *
   * @param <T> the type of elements of the iterator
   * @deprecated use {@code mergediterator()}
   */
  @Deprecated // make package-private
  public static final class MergedIterator<T> implements Iterator<T> {

    /** The iterators that this object merges. */
    Iterator<Iterator<T>> itorOfItors;

    /**
     * Create an iterator that returns the elements of the given iterators, in turn.
     *
     * @param itorOfItors an iterator whose elements are iterators; this MergedIterator will merge
     *     them all
     * @deprecated use {@link mergedIterator(Iterator)}
     */
    @Deprecated // make package-private
    public MergedIterator(Iterator<Iterator<T>> itorOfItors) {
      this.itorOfItors = itorOfItors;
    }

    /** The current iterator (from {@link #itorOfItors}) that is being iterated over. */
    // Initialize to an empty iterator to prime the pump.
    Iterator<T> current = new ArrayList<T>().iterator();

    @SuppressWarnings({"allcheckers:purity", "lock:method.guarantee.violated"})
    @Override
    public boolean hasNext(@GuardSatisfied MergedIterator<T> this) {
      while (!current.hasNext() && itorOfItors.hasNext()) {
        current = itorOfItors.next();
      }
      return current.hasNext();
    }

    @Override
    public T next(@GuardSatisfied MergedIterator<T> this) {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      return current.next();
    }

    @Override
    public void remove(@GuardSatisfied MergedIterator<T> this) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Returns an iterator that only returns elements of {@code itor} that match the given predicate.
   *
   * @param <T> the type of elements of the iterator
   * @param itor the Iterator to filter
   * @param predicate the predicate that determines which elements to retain
   * @return an iterator that only returns elements of {@code itor} that match the given predicate
   */
  public static <T> Iterator<T> filteredIterator(Iterator<T> itor, Predicate<T> predicate) {
    return new FilteredIterator<>(itor, predicate);
  }

  /**
   * An iterator that only returns elements that match the given predicate.
   *
   * @param <T> the type of elements of the iterator
   * @deprecated use {@link #filteredIterator}
   */
  @Deprecated // make package-private
  public static final class FilteredIterator<T> implements Iterator<T> {
    /** The iterator that this object is filtering. */
    Iterator<T> itor;

    /** The predicate that determines which elements to retain. */
    Predicate<T> predicate;

    /**
     * Create an iterator that only returns elements of {@code itor} that match the given predicate.
     *
     * @param itor the Iterator to filter
     * @param predicate the predicate that determines which elements to retain
     * @deprecated use {@link #filteredIterator}
     */
    @Deprecated // make package-private
    public FilteredIterator(Iterator<T> itor, Predicate<T> predicate) {
      this.itor = itor;
      this.predicate = predicate;
    }

    /** A marker object, distinct from any object that the iterator can return. */
    @SuppressWarnings("unchecked")
    T invalidT = (T) new Object();

    /**
     * The next object that this iterator will yield, or {@link #invalidT} if {@link #currentValid}
     * is false.
     */
    T current = invalidT;

    /** True iff {@link #current} is an object from the wrapped iterator. */
    boolean currentValid = false;

    @SuppressWarnings({
      "allcheckers:purity",
      "lock:method.guarantee.violated"
    }) // benevolent side effects
    @Override
    public boolean hasNext(@GuardSatisfied FilteredIterator<T> this) {
      while (!currentValid && itor.hasNext()) {
        current = itor.next();
        currentValid = predicate.test(current);
      }
      return currentValid;
    }

    @Override
    public T next(@GuardSatisfied FilteredIterator<T> this) {
      if (hasNext()) {
        currentValid = false;
        @SuppressWarnings("interning")
        boolean ok = (current != invalidT);
        assert ok;
        return current;
      } else {
        throw new NoSuchElementException();
      }
    }

    @Override
    public void remove(@GuardSatisfied FilteredIterator<T> this) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Returns an iterator just like {@code itor}, except without its first and last elements.
   *
   * @param <T> the type of elements of the iterator
   * @param itor an itorator whose first and last elements to discard
   * @return an iterator just like {@code itor}, except without its first and last elements
   */
  public static <T extends @Nullable Object> Iterator<T> removeFirstAndLastIterator(
      Iterator<T> itor) {
    return new RemoveFirstAndLastIterator<>(itor);
  }

  /**
   * Returns an iterator just like its argument, except that the first and last elements are
   * removed. They can be accessed via the {@link #getFirst} and {@link #getLast} methods.
   *
   * @param <T> the type of elements of the iterator
   * @deprecated use {@link #removeFirstAndLastIterator}
   */
  @Deprecated // make package-private
  public static final class RemoveFirstAndLastIterator<T> implements Iterator<T> {
    /** The wrapped iterator. */
    Iterator<T> itor;

    /** A marker object, distinct from any object that the iterator can return. */
    @SuppressWarnings("unchecked")
    T nothing = (T) new Object();

    // I don't think this works, because the iterator might itself return null
    // @Nullable T nothing = (@Nullable T) null;

    /** The first object yielded by the wrapped iterator. */
    T first = nothing;

    /** The next object that this iterator will return. */
    T current = nothing;

    /**
     * Create an iterator just like {@code itor}, except without its first and last elements.
     *
     * @param itor an itorator whose first and last elements to discard
     * @deprecated use {@link #removeFirstAndLastIterator}
     */
    @Deprecated // make package-private
    public RemoveFirstAndLastIterator(Iterator<T> itor) {
      this.itor = itor;
      if (itor.hasNext()) {
        first = itor.next();
      }
      if (itor.hasNext()) {
        current = itor.next();
      }
    }

    @Override
    public boolean hasNext(@GuardSatisfied RemoveFirstAndLastIterator<T> this) {
      return itor.hasNext();
    }

    @Override
    public T next(@GuardSatisfied RemoveFirstAndLastIterator<T> this) {
      if (!itor.hasNext()) {
        throw new NoSuchElementException();
      }
      T tmp = current;
      current = itor.next();
      return tmp;
    }

    /**
     * Returns the first element of the iterator that was used to construct this. This value is not
     * part of this iterator (unless the original iterator would have returned it multiple times).
     *
     * @return the first element of the iterator that was used to construct this
     */
    @SuppressWarnings("allcheckers:purity.not.sideeffectfree.call") // constructing an exception
    @Pure
    public T getFirst() {
      @SuppressWarnings("interning") // check for equality to a special value
      boolean invalid = (first == nothing);
      if (invalid) {
        throw new NoSuchElementException();
      }
      return first;
    }

    /**
     * Returns the last element of the iterator that was used to construct this. This value is not
     * part of this iterator (unless the original iterator would have returned it multiple times).
     *
     * <p>Throws an error unless the RemoveFirstAndLastIterator has already been iterated all the
     * way to its end (so the delegate is pointing to the last element).
     *
     * @return the last element of the iterator that was used to construct this
     */
    // TODO: This is buggy when the delegate is empty.
    @Pure
    public T getLast() {
      if (itor.hasNext()) {
        throw new Error();
      }
      return current;
    }

    @Override
    public void remove(@GuardSatisfied RemoveFirstAndLastIterator<T> this) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Returns a List containing numElts randomly chosen elements from the iterator, or all the
   * elements of the iterator if there are fewer. It examines every element of the iterator, but
   * does not keep them all in memory.
   *
   * @param <T> type of the iterator elements
   * @param itor elements to be randomly selected from
   * @param numElts number of elements to select
   * @return list of numElts elements from itor
   */
  public static <T extends @Nullable Object> List<T> randomElements(Iterator<T> itor, int numElts) {
    return randomElements(itor, numElts, r);
  }

  /** The random generator. */
  private static Random r = new Random();

  /**
   * Returns a List containing numElts randomly chosen elements from the iterator, or all the
   * elements of the iterator if there are fewer. It examines every element of the iterator, but
   * does not keep them all in memory.
   *
   * @param <T> type of the iterator elements
   * @param itor elements to be randomly selected from
   * @param numElts number of elements to select
   * @param random the Random instance to use to make selections
   * @return list of numElts elements from itor
   */
  public static <T> List<T> randomElements(Iterator<T> itor, int numElts, Random random) {
    // The elements are chosen with the following probabilities,
    // where n == numElts:
    //   n n/2 n/3 n/4 n/5 ...

    RandomSelector<T> rs = new RandomSelector<>(numElts, random);

    while (itor.hasNext()) {
      rs.accept(itor.next());
    }
    return rs.getValues();

    /*
    ArrayList<T> result = new ArrayList<>(numElts);
    int i=1;
    for (int n=0; n<numElts && itor.hasNext(); n++, i++) {
      result.add(itor.next());
    }
    for (; itor.hasNext(); i++) {
      T o = itor.next();
      // test random < numElts/i
      if (random.nextDouble() * i < numElts) {
        // This element will replace one of the existing elements.
        result.set(random.nextInt(numElts), o);
      }
    }
    return result;

    */
  }

  // //////////////////////////////////////////////////////////////////////
  // Map
  //

  // In Python, inlining this gave a 10x speed improvement.
  // Will the same be true for Java?
  /**
   * Increments the Integer which is indexed by key in the Map. Sets the value to 1 if not currently
   * mapped.
   *
   * @param <K> type of keys in the map
   * @param m map from K to Integer
   * @param key the key whose value will be incremented
   * @return the old value, before it was incremented; this might be null
   * @throws Error if the key is in the Map but maps to a non-Integer
   * @deprecated use {@link MapsP#incrementMap}
   */
  @Deprecated // 2025-06-28
  public static <K extends @NonNull Object> @Nullable Integer incrementMap(
      Map<K, Integer> m, K key) {
    return incrementMap(m, key, 1);
  }

  /**
   * Increments the Integer which is indexed by key in the Map. Sets the value to {@code count} if
   * not currently mapped.
   *
   * @param <K> type of keys in the map
   * @param m map from K to Integer
   * @param key the key whose value will be incremented
   * @param count how much to increment the value by
   * @return the old value, before it was incremented; this might be null
   * @throws Error if the key is in the Map but maps to a non-Integer
   * @deprecated use {@link MapsP#incrementMap}
   */
  @Deprecated // 2025-06-28
  public static <K extends @NonNull Object> @Nullable Integer incrementMap(
      Map<K, Integer> m, K key, int count) {
    Integer newTotal = m.getOrDefault(key, 0) + count;
    return m.put(key, newTotal);
  }

  /**
   * Returns a sorted version of m.keySet().
   *
   * @param <K> type of the map keys
   * @param <V> type of the map values
   * @param m a map whose keyset will be sorted
   * @return a sorted version of m.keySet()
   * @deprecated use {@link MapsP#sortedKeySet}
   */
  @Deprecated // 2025-06-28
  public static <K extends Comparable<? super K>, V> Collection<@KeyFor("#1") K> sortedKeySet(
      Map<K, V> m) {
    ArrayList<@KeyFor("#1") K> theKeys = new ArrayList<>(m.keySet());
    Collections.sort(theKeys);
    return theKeys;
  }

  /**
   * Returns a sorted version of m.keySet().
   *
   * @param <K> type of the map keys
   * @param <V> type of the map values
   * @param m a map whose keyset will be sorted
   * @param comparator the Comparator to use for sorting
   * @return a sorted version of m.keySet()
   * @deprecated use {@link MapsP#sortedKeySet}
   */
  @Deprecated // 2025-06-28
  public static <K, V> Collection<@KeyFor("#1") K> sortedKeySet(
      Map<K, V> m, Comparator<K> comparator) {
    ArrayList<@KeyFor("#1") K> theKeys = new ArrayList<>(m.keySet());
    Collections.sort(theKeys, comparator);
    return theKeys;
  }

  /**
   * Given an expected number of elements, returns the capacity that should be passed to a HashMap
   * or HashSet constructor, so that the set or map will not resize.
   *
   * @param numElements the maximum expected number of elements in the map or set
   * @return the initial capacity to pass to a HashMap or HashSet constructor
   * @deprecated use {@link MapsP#mapCapacity}
   */
  @Deprecated // 2025-06-28
  public static int mapCapacity(int numElements) {
    // Equivalent to: (int) (numElements / 0.75) + 1
    // where 0.75 is the default load factor used throughout the JDK.
    return (numElements * 4 / 3) + 1;
  }

  /**
   * Given an array, returns the capacity that should be passed to a HashMap or HashSet constructor,
   * so that the set or map will not resize.
   *
   * @param <T> the type of elements of the array
   * @param a an array whose length is the maximum expected number of elements in the map or set
   * @return the initial capacity to pass to a HashMap or HashSet constructor
   * @deprecated use {@link MapsP#mapCapacity}
   */
  @Deprecated // 2025-06-28
  public static <T> int mapCapacity(T[] a) {
    return mapCapacity(a.length);
  }

  /**
   * Given a collection, returns the capacity that should be passed to a HashMap or HashSet
   * constructor, so that the set or map will not resize.
   *
   * @param c a collection whose size is the maximum expected number of elements in the map or set
   * @return the initial capacity to pass to a HashMap or HashSet constructor
   * @deprecated use {@link MapsP#mapCapacity}
   */
  @Deprecated // 2025-06-28
  public static int mapCapacity(Collection<?> c) {
    return mapCapacity(c.size());
  }

  /**
   * Given a map, returns the capacity that should be passed to a HashMap or HashSet constructor, so
   * that the set or map will not resize.
   *
   * @param m a map whose size is the maximum expected number of elements in the map or set
   * @return the initial capacity to pass to a HashMap or HashSet constructor
   * @deprecated use {@link MapsP#mapCapacity}
   */
  @Deprecated // 2025-06-28
  public static int mapCapacity(Map<?, ?> m) {
    return mapCapacity(m.size());
  }

  // The following two methods cannot share an implementation because their generic bounds differ.

  /**
   * Returns a copy of {@code orig}, where each key and value in the result is a deep copy
   * (according to the {@code DeepCopyable} interface) of the corresponding element of {@code orig}.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param <M> the type of the map
   * @param orig a map
   * @return a copy of {@code orig}, as described above
   * @deprecated use {@link MapsP#deepCopy}
   */
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  @Deprecated // 2025-06-28
  public static <
          K extends @Nullable DeepCopyable<K>,
          V extends @Nullable DeepCopyable<V>,
          M extends @Nullable Map<K, V>>
      @PolyNull M deepCopy(@PolyNull M orig) {
    if (orig == null) {
      return null;
    }
    M result = UtilPlume.clone(orig);
    result.clear();
    for (Map.Entry<K, V> mapEntry : orig.entrySet()) {
      K oldKey = mapEntry.getKey();
      V oldValue = mapEntry.getValue();
      result.put(DeepCopyable.deepCopyOrNull(oldKey), DeepCopyable.deepCopyOrNull(oldValue));
    }
    return result;
  }

  /**
   * Returns a copy of {@code orig}, where each value of the result is a deep copy (according to the
   * {@code DeepCopyable} interface) of the corresponding value of {@code orig}, but the keys are
   * the same objects.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param <M> the type of the map
   * @param orig a map
   * @return a copy of {@code orig}, as described above
   * @deprecated use {@link MapsP#deepCopyValues}
   */
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  @Deprecated // 2025-06-28
  public static <K, V extends @Nullable DeepCopyable<V>, M extends @Nullable Map<K, V>>
      @PolyNull M deepCopyValues(@PolyNull M orig) {
    if (orig == null) {
      return null;
    }
    M result = UtilPlume.clone(orig);
    result.clear();
    for (Map.Entry<K, V> mapEntry : orig.entrySet()) {
      K oldKey = mapEntry.getKey();
      V oldValue = mapEntry.getValue();
      result.put(oldKey, DeepCopyable.deepCopyOrNull(oldValue));
    }
    return result;
  }

  /**
   * Creates a LRU cache.
   *
   * <p>You might want to consider using a {@code WeakHashMap} or {@code WeakIdentityHashMap}
   * instead
   *
   * @param <K> the type of keys
   * @param <V> the type of values
   * @param size size of the cache
   * @return a new cache with the provided size
   * @deprecated use {@link MapsP#createLruCache}
   */
  @Deprecated // 2025-06-28
  public static <K, V> Map<K, V> createLruCache(@Positive int size) {
    return new LinkedHashMap<K, V>(size, .75F, true) {

      private static final long serialVersionUID = 5261489276168775084L;

      @SuppressWarnings(
          "lock:override.receiver") // cannot write receiver parameter within an anonymous class
      @Override
      protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > size;
      }
    };
  }

  /**
   * Returns a copy of {@code orig}, where each key and value in the result is a clone of the
   * corresponding element of {@code orig}.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param <M> the type of the map
   * @param orig a map
   * @return a copy of {@code orig}, as described above
   * @deprecated use {@link MapsP#cloneElements}
   */
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  @Deprecated // 2025-06-28
  public static <K, V, M extends @Nullable Map<K, V>> @PolyNull M cloneElements(@PolyNull M orig) {
    return cloneElements(orig, true);
  }

  /**
   * Returns a copy of {@code orig}, where each value of the result is a clone of the corresponding
   * value of {@code orig}, but the keys are the same objects.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param <M> the type of the map
   * @param orig a map
   * @return a copy of {@code orig}, as described above
   * @deprecated use {@link MapsP#cloneValues}
   */
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  @Deprecated // 2025-06-28
  public static <K, V, M extends @Nullable Map<K, V>> @PolyNull M cloneValues(@PolyNull M orig) {
    return cloneElements(orig, false);
  }

  /**
   * Returns a copy of {@code orig}, where each key and value in the result is a clone of the
   * corresponding element of {@code orig}.
   *
   * @param <K> the type of keys of the map
   * @param <V> the type of values of the map
   * @param <M> the type of the map
   * @param orig a map
   * @param cloneKeys if true, clone keys; otherwise, re-use them
   * @return a copy of {@code orig}, as described above
   */
  @SuppressWarnings({"nullness", "signedness"}) // generics problem with clone
  private static <K, V, M extends @Nullable Map<K, V>> @PolyNull M cloneElements(
      @PolyNull M orig, boolean cloneKeys) {
    if (orig == null) {
      return null;
    }
    M result = UtilPlume.clone(orig);
    result.clear();
    for (Map.Entry<K, V> mapEntry : orig.entrySet()) {
      K oldKey = mapEntry.getKey();
      K newKey = cloneKeys ? UtilPlume.clone(oldKey) : oldKey;
      result.put(newKey, UtilPlume.clone(mapEntry.getValue()));
    }
    return result;
  }

  //
  // Map to string
  //

  // First, versions that append to an Appendable.

  /**
   * Write a multi-line representation of the map into the given Appendable (e.g., a StringBuilder),
   * including a final line separator (unless the map is empty).
   *
   * <p>This is less expensive than {@code sb.append(mapToStringMultiLine(m))}.
   *
   * @param <K> type of map keys
   * @param <V> type of map values
   * @param sb an Appendable (such as StringBuilder) to which to write a multi-line string
   *     representation of m
   * @param m map to be converted to a string
   * @param linePrefix a prefix to put at the beginning of each line
   * @deprecated use {@link MapsP#mapToString}
   */
  @Deprecated // 2025-06-28
  public static <K extends @Signed @Nullable Object, V extends @Signed @Nullable Object>
      void mapToString(Appendable sb, Map<K, V> m, String linePrefix) {
    mapToStringMultiLine(sb, m, linePrefix);
  }

  /**
   * Write a multi-line representation of the map into the given Appendable (e.g., a StringBuilder),
   * including a final line separator (unless the map is empty).
   *
   * <p>This is less expensive than {@code sb.append(mapToStringMultiLine(m))}.
   *
   * @param <K> type of map keys
   * @param <V> type of map values
   * @param sb an Appendable (such as StringBuilder) to which to write a multi-line string
   *     representation of m
   * @param m map to be converted to a string
   * @param linePrefix a prefix to put at the beginning of each line
   * @deprecated use {@link MapsP#mapToStringMultiLine}
   */
  @Deprecated // 2025-06-28
  public static <K extends @Signed @Nullable Object, V extends @Signed @Nullable Object>
      void mapToStringMultiLine(Appendable sb, Map<K, V> m, String linePrefix) {
    try {
      for (Map.Entry<K, V> entry : m.entrySet()) {
        sb.append(linePrefix);
        sb.append(Objects.toString(entry.getKey()));
        sb.append(" => ");
        sb.append(Objects.toString(entry.getValue()));
        sb.append(lineSep);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Write a multi-line representation of the map of maps into the given Appendable (e.g., a
   * StringBuilder), including a final line separator (unless the map is empty).
   *
   * @param <K1> the type of the outer map keys
   * @param <K2> the type of the inner map keys
   * @param <V2> the type of the inner map values
   * @param sb the destination for the string representation
   * @param linePrefix a prefix to put at the beginning of each line
   * @param innerHeader what to print before each key of the outer map (equivalently, before each
   *     each inner map). If non-empty, it usually ends with a space to avoid abutting the outer map
   *     key.
   * @param mapMap what to print
   */
  static <K1 extends @Signed Object, K2 extends @Signed Object, V2 extends @Signed Object>
      void mapMapToStringMultiLine(
          Appendable sb, String innerHeader, Map<K1, Map<K2, V2>> mapMap, String linePrefix) {
    try {
      for (Map.Entry<K1, Map<K2, V2>> entry : mapMap.entrySet()) {
        sb.append(linePrefix);
        sb.append(innerHeader);
        sb.append(Objects.toString(entry.getKey()));
        sb.append(lineSep);
        mapToStringMultiLine(sb, entry.getValue(), linePrefix + "  ");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // Second, versions that return a String.

  /**
   * Returns a multi-line string representation of a map.
   *
   * @param <K> type of map keys
   * @param <V> type of map values
   * @param m map to be converted to a string
   * @return a multi-line string representation of m
   * @deprecated use {@link MapsP#mapToString}
   */
  @Deprecated // 2025-06-28
  @SideEffectFree
  public static <K extends @Signed @Nullable Object, V extends @Signed @Nullable Object>
      String mapToString(Map<K, V> m) {
    return mapToStringMultiLine(m);
  }

  /**
   * Returns a multi-line string representation of a map. Each key-value pair appears on its own
   * line, with no indentation. The last line does not end with a line separator.
   *
   * @param <K> type of map keys
   * @param <V> type of map values
   * @param m map to be converted to a string
   * @return a multi-line string representation of the map
   * @deprecated use {@link MapsP#mapToStringMultiLine}
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // side effect to local state
  })
  @SideEffectFree
  @Deprecated // 2025-06-28
  public static <K extends @Signed @Nullable Object, V extends @Signed @Nullable Object>
      String mapToStringMultiLine(Map<K, V> m) {
    StringJoiner result = new StringJoiner(lineSep);
    for (Map.Entry<K, V> e : m.entrySet()) {
      result.add(e.getKey() + " => " + e.getValue());
    }
    return result.toString();
  }

  /**
   * Returns a multi-line string representation of a map. Each key-value pair appears on its own
   * line, with no indentation. The last line does not end with a line separator.
   *
   * @param <K> type of map keys
   * @param <V> type of map values
   * @param m map to be converted to a string
   * @param linePrefix a prefix to put at the beginning of each line
   * @return a multi-line string representation of the map
   * @deprecated use {@link MapsP#mapToStringMultiLine}
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // side effect to local state
  })
  @SideEffectFree
  @Deprecated // 2025-06-28
  public static <K extends @Signed @Nullable Object, V extends @Signed @Nullable Object>
      String mapToStringMultiLine(Map<K, V> m, String linePrefix) {
    StringJoiner result = new StringJoiner(lineSep);
    for (Map.Entry<K, V> e : m.entrySet()) {
      result.add(linePrefix + e.getKey() + " => " + e.getValue());
    }
    return result.toString();
  }

  /**
   * Convert a map to a multi-line string representation, which includes the runtime class of keys
   * and values. The last line does not end with a line separator.
   *
   * @param <K> type of map keys
   * @param <V> type of map values
   * @param m a map
   * @return a string representation of the map
   * @deprecated use {@link MapsP#mapToStringAndClassMultiLine}
   */
  @SideEffectFree
  @Deprecated // 2025-06-28
  public static <K extends @Signed @Nullable Object, V extends @Signed @Nullable Object>
      String mapToStringAndClassMultiLine(Map<K, V> m) {
    return mapToStringAndClassMultiLine(m, "");
  }

  /**
   * Convert a map to a multi-line string representation, which includes the runtime class of keys
   * and values. The last line does not end with a line separator.
   *
   * @param <K> type of map keys
   * @param <V> type of map values
   * @param m a map
   * @param linePrefix a prefix to put at the beginning of each line
   * @return a string representation of the map
   * @deprecated use {@link MapsP#mapToStringAndClassMultiLine}
   */
  @SuppressWarnings({
    "allcheckers:purity.not.sideeffectfree.call", // side effect to local state
    "lock:method.guarantee.violated" // side effect to local state
  })
  @SideEffectFree
  @Deprecated // 2025-06-28
  public static <K extends @Signed @Nullable Object, V extends @Signed @Nullable Object>
      String mapToStringAndClassMultiLine(Map<K, V> m, String linePrefix) {
    StringJoiner result = new StringJoiner(lineSep);
    for (Map.Entry<K, V> e : m.entrySet()) {
      result.add(
          linePrefix
              + StringsPlume.toStringAndClass(e.getKey())
              + " => "
              + StringsPlume.toStringAndClass(e.getValue()));
    }
    return result.toString();
  }

  // //////////////////////////////////////////////////////////////////////
  // Set
  //

  /**
   * Returns the object in the given set that is equal to key. The Set abstraction doesn't provide
   * this; it only provides "contains". Returns null if the argument is null, or if it isn't in the
   * set.
   *
   * @param set a set in which to look up the value
   * @param key the value to look up in the set
   * @return the object in the given set that is equal to key, or null
   */
  public static @Nullable Object getFromSet(Set<? extends @Nullable Object> set, Object key) {
    if (key == null) {
      return null;
    }
    for (Object elt : set) {
      if (key.equals(elt)) {
        return elt;
      }
    }
    return null;
  }

  /**
   * Adds an element to the given collection, but only if it is not already present.
   *
   * @param <T> the type of the collection elements
   * @param c a collection to be added to; is side-effected by this method
   * @param e an element to add to the collection
   * @return true if the collection c changed (that is, if an element was added)
   */
  @SuppressWarnings("nullness:argument") // c might forbid null
  public static <T> boolean adjoin(Collection<T> c, T e) {
    if (!c.contains(e)) {
      c.add(e);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Adds elements to the given collection, but only ones that are not already present.
   *
   * <p>This method could alternately be named "union".
   *
   * @param <T> the type of the collection elements
   * @param c a collection to be added to; is side-effected by this method
   * @param toAdd elements to add to the collection, if they are not already present
   * @return true if the collection c changed (that is, if an element was added)
   */
  @SuppressWarnings("nullness:argument") // c might forbid null
  public static <T> boolean adjoinAll(Collection<T> c, Collection<? extends T> toAdd) {
    boolean result = false;
    for (T e : toAdd) {
      if (!c.contains(e)) {
        c.add(e);
        result = true;
      }
    }
    return result;
  }

  /**
   * Returns a new list that is the union of the given collections. The given lists should be small,
   * since the cost of this method is O(c1.size() * c2.size()). For small lists, this is more
   * efficient than creating and using a Set.
   *
   * @param <T> the type of the collection elements
   * @param c1 the first collection
   * @param c2 the second collection
   * @return a duplicate-free list that is the union of the given collections
   */
  public static <T> List<T> listUnion(Collection<T> c1, Collection<T> c2) {
    // TODO: use a set if the collection sizes are big enough, to avoid quadratic time complexity.
    List<T> result = new ArrayList<>(c1.size() + c2.size());
    adjoinAll(result, c1);
    adjoinAll(result, c2);
    return result;
  }

  /**
   * Returns a new list that is the intersection of the given collections. The given lists should be
   * small, since the cost of this method is O(c1.size() * c2.size()). For small lists, this is more
   * efficient than creating and using a Set.
   *
   * @param <T> the type of the collection elements
   * @param c1 the first collection
   * @param c2 the second collection
   * @return a duplicate-free list that is the union of the given collections
   */
  public static <T> List<T> listIntersection(Collection<T> c1, Collection<T> c2) {
    // TODO: use a set if the collection sizes are big enough, to avoid quadratic time complexity.
    List<T> result = new ArrayList<>(c1.size());
    adjoinAll(result, c1);
    result.retainAll(c2);
    return result;
  }

  // //////////////////////////////////////////////////////////////////////
  // BitSet
  //

  /**
   * Returns true if the cardinality of the intersection of the two BitSets is at least the given
   * value.
   *
   * @param a the first BitSet to intersect
   * @param b the second BitSet to intersect
   * @param i the cardinality bound
   * @return true iff size(a intersect b) &ge; i
   */
  @SuppressWarnings({"allcheckers:purity", "lock"}) // side effect to local state (BitSet)
  @Pure
  public static boolean intersectionCardinalityAtLeast(BitSet a, BitSet b, @NonNegative int i) {
    // Here are three implementation strategies to determine the
    // cardinality of the intersection:
    // 1. a.clone().and(b).cardinality()
    // 2. do the above, but copy only a subset of the bits initially -- enough
    //    that it should exceed the given number -- and if that fails, do the
    //    whole thing.  Unfortunately, bits.get(int, int) isn't optimized
    //    for the case where the indices line up, so I'm not sure at what
    //    point this approach begins to dominate #1.
    // 3. iterate through both sets with nextSetBit()

    int size = Math.min(a.length(), b.length());
    if (size > 10 * i) {
      // The size is more than 10 times the limit.  So first try processing
      // just a subset of the bits (4 times the limit).
      BitSet intersection = a.get(0, 4 * i);
      intersection.and(b);
      if (intersection.cardinality() >= i) {
        return true;
      }
    }
    return intersectionCardinality(a, b) >= i;
  }

  /**
   * Returns true if the cardinality of the intersection of the three BitSets is at least the given
   * value.
   *
   * @param a the first BitSet to intersect
   * @param b the second BitSet to intersect
   * @param c the third BitSet to intersect
   * @param i the cardinality bound
   * @return true iff size(a intersect b intersect c) &ge; i
   */
  @SuppressWarnings({"allcheckers:purity", "lock"}) // side effect to local state (BitSet)
  @Pure
  public static boolean intersectionCardinalityAtLeast(
      BitSet a, BitSet b, BitSet c, @NonNegative int i) {
    // See comments in intersectionCardinalityAtLeast(BitSet, BitSet, int).
    // This is a copy of that.

    int size = Math.min(a.length(), b.length());
    size = Math.min(size, c.length());
    if (size > 10 * i) {
      // The size is more than 10 times the limit.  So first try processing
      // just a subset of the bits (4 times the limit).
      BitSet intersection = a.get(0, 4 * i);
      intersection.and(b);
      intersection.and(c);
      if (intersection.cardinality() >= i) {
        return true;
      }
    }
    return intersectionCardinality(a, b, c) >= i;
  }

  /**
   * Returns the cardinality of the intersection of the two BitSets.
   *
   * @param a the first BitSet to intersect
   * @param b the second BitSet to intersect
   * @return size(a intersect b)
   */
  @SuppressWarnings({"allcheckers:purity", "lock"}) // side effect to local state (BitSet)
  @Pure
  public static int intersectionCardinality(BitSet a, BitSet b) {
    BitSet intersection = (BitSet) a.clone();
    intersection.and(b);
    return intersection.cardinality();
  }

  /**
   * Returns the cardinality of the intersection of the three BitSets.
   *
   * @param a the first BitSet to intersect
   * @param b the second BitSet to intersect
   * @param c the third BitSet to intersect
   * @return size(a intersect b intersect c)
   */
  @SuppressWarnings({"allcheckers:purity", "lock"}) // side effect to local state (BitSet)
  @Pure
  public static int intersectionCardinality(BitSet a, BitSet b, BitSet c) {
    BitSet intersection = (BitSet) a.clone();
    intersection.and(b);
    intersection.and(c);
    return intersection.cardinality();
  }
}
