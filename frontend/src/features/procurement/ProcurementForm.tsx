/* oxlint-disable react/incompatible-library -- react-hook-form owns mutable form subscriptions by design. */
import { useFieldArray, useForm } from 'react-hook-form'

export type ProcurementDraftInput = {
  title: string
  purpose: string
  budgetCode: string
  items: Array<{
    name: string
    quantity: number
    unitPriceKrw: number
  }>
}

type ProcurementFormProps = {
  onCreate: (input: ProcurementDraftInput) => void | Promise<void>
  busy?: boolean
}

const numberFormatter = new Intl.NumberFormat('ko-KR')

export function ProcurementForm({ onCreate, busy = false }: ProcurementFormProps) {
  const {
    control,
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<ProcurementDraftInput>({
    defaultValues: {
      title: '',
      purpose: '',
      budgetCode: '',
      items: [{ name: '', quantity: 1, unitPriceKrw: 0 }],
    },
  })
  const { fields, append, remove } = useFieldArray({ control, name: 'items' })
  const items = watch('items')
  const total = items.reduce(
    (sum, item) => sum + (Number(item.quantity) || 0) * (Number(item.unitPriceKrw) || 0),
    0,
  )

  return (
    <form
      className="procurement-form"
      onSubmit={handleSubmit((values) => onCreate(values))}
    >
      <div className="section-heading">
        <div>
          <span className="eyebrow">PROCUREMENT / NEW REQUEST</span>
          <h2>구매 요청 작성</h2>
        </div>
        <span className="document-mark">DRAFT</span>
      </div>

      <div className="form-grid form-grid--summary">
        <label>
          <span>요청 제목</span>
          <input {...register('title', { required: '요청 제목을 입력하세요.' })} />
          {errors.title && <small role="alert">{errors.title.message}</small>}
        </label>
        <label>
          <span>예산 코드</span>
          <input {...register('budgetCode', { required: '예산 코드를 입력하세요.' })} />
          {errors.budgetCode && <small role="alert">{errors.budgetCode.message}</small>}
        </label>
      </div>

      <label>
        <span>사용 목적</span>
        <textarea rows={4} {...register('purpose', { required: '사용 목적을 입력하세요.' })} />
        {errors.purpose && <small role="alert">{errors.purpose.message}</small>}
      </label>

      <div className="line-items">
        <div className="line-items__header">
          <span>품목 구성</span>
          <button
            className="text-button"
            type="button"
            onClick={() => append({ name: '', quantity: 1, unitPriceKrw: 0 })}
          >
            + 품목 추가
          </button>
        </div>
        {fields.map((field, index) => (
          <div className="line-item" key={field.id}>
            <span className="line-item__number">{String(index + 1).padStart(2, '0')}</span>
            <label>
              <span>품목명</span>
              <input {...register(`items.${index}.name`, { required: true })} />
            </label>
            <label>
              <span>수량</span>
              <input
                type="number"
                min={1}
                {...register(`items.${index}.quantity`, { valueAsNumber: true, min: 1 })}
              />
            </label>
            <label>
              <span>단가</span>
              <input
                type="number"
                min={0}
                step={1}
                {...register(`items.${index}.unitPriceKrw`, { valueAsNumber: true, min: 0 })}
              />
            </label>
            <div className="line-item__subtotal">
              <span>소계</span>
              <strong>
                {numberFormatter.format(
                  (Number(items[index]?.quantity) || 0)
                    * (Number(items[index]?.unitPriceKrw) || 0),
                )}원
              </strong>
            </div>
            {fields.length > 1 && (
              <button
                className="icon-button"
                type="button"
                aria-label={`${index + 1}번 품목 삭제`}
                onClick={() => remove(index)}
              >
                ×
              </button>
            )}
          </div>
        ))}
      </div>

      <div className="form-total">
        <span>서버 재검산 예정 금액</span>
        <strong>{numberFormatter.format(total)}원</strong>
      </div>

      <div className="form-actions">
        <p>조직·요청자·상태·합계는 로그인 세션과 서버 규칙으로 결정됩니다.</p>
        <button className="primary-button" type="submit" disabled={busy}>
          {busy ? '저장 중…' : '임시 저장'}
        </button>
      </div>
    </form>
  )
}
